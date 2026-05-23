import axios from 'axios';
import type { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { STORAGE_ADMIN_TOKEN, STORAGE_CUSTOMER_TOKEN } from '@/constants/storageKeys';
import { appLogger } from '@/utils/appLogger';

type ConfigWithMeta = InternalAxiosRequestConfig & { metadata?: { startedAt: number } };

const baseURL = import.meta.env.VITE_API_URL ?? '/api';

const sharedConfig = {
  baseURL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 60_000,
} as const;

export const api = axios.create(sharedConfig);

/**
 * Cliente Axios para rotas que exigem `X-Admin-Key` (alinhado a `app.admin.key` no backend).
 * Interceptor envia `VITE_ADMIN_KEY` e trata 403 com mensagem explícita.
 */
export const adminApi = axios.create(sharedConfig);

/** Spring ProblemDetail (RFC 7807) e formatos comuns de erro da API */
function messageFromResponseData(data: unknown): string | undefined {
  if (data == null) return undefined;
  if (typeof data === 'string') return data;
  if (typeof data !== 'object') return undefined;
  const d = data as Record<string, unknown>;
  if (typeof d.detail === 'string' && d.detail.trim()) return d.detail;
  if (typeof d.message === 'string') return d.message;
  if (typeof d.error === 'string') return d.error;
  if (Array.isArray(d.errors) && d.errors.length > 0) {
    const first = d.errors[0];
    if (typeof first === 'string') return first;
    if (first && typeof first === 'object' && 'defaultMessage' in first) {
      return String((first as { defaultMessage?: string }).defaultMessage ?? '');
    }
  }
  return undefined;
}

function rejectWithParsedError(err: AxiosError<unknown>, opts?: { admin403?: boolean }): Promise<never> {
  const cfg = err.config as ConfigWithMeta | undefined;
  const method = (cfg?.method ?? 'GET').toUpperCase();
  const url = cfg?.url ?? '(sem url)';
  const status = err.response?.status;
  const started = cfg?.metadata?.startedAt;
  const ms = started != null ? Math.round(performance.now() - started) : undefined;

  if (opts?.admin403 && err.response?.status === 403) {
    appLogger.warn('ApiClient', 'responseError', `${method} ${url} falhou`, {
      status: 403,
      durationMs: ms,
      reason: 'admin_key',
    });
    return Promise.reject(
      new Error(
        'Acesso negado (403). Defina VITE_ADMIN_KEY no front igual a app.admin.key no backend (header X-Admin-Key).',
      ),
    );
  }
  const fromBody = err.response?.data != null ? messageFromResponseData(err.response.data) : undefined;
  const raw = fromBody ?? err.message ?? 'Erro de rede';
  const msg = Array.isArray(raw) ? raw.join(', ') : String(raw);
  appLogger.warn('ApiClient', 'responseError', `${method} ${url} falhou`, {
    status: status ?? 'network',
    durationMs: ms,
    message: msg.length > 200 ? `${msg.slice(0, 200)}…` : msg,
  });
  // #region agent log
  if (method === 'POST' && url.includes('/auth/login')) {
    fetch('http://127.0.0.1:7449/ingest/fe3f462e-f2fd-41a9-818d-3105cdb7b029', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Debug-Session-Id': '4b6485' },
      body: JSON.stringify({
        sessionId: '4b6485',
        hypothesisId: 'H2',
        location: 'api.ts:rejectWithParsedError',
        message: 'POST /auth/login',
        data: { status: status ?? null, msgPrefix: msg.slice(0, 120) },
        timestamp: Date.now(),
      }),
    }).catch(() => {});
  }
  // #endregion
  return Promise.reject(new Error(msg));
}

function attachTiming(config: InternalAxiosRequestConfig): ConfigWithMeta {
  const c = config as ConfigWithMeta;
  c.metadata = { startedAt: performance.now() };
  return c;
}

function logSuccessResponse(config: ConfigWithMeta, status: number): void {
  const method = (config.method ?? 'GET').toUpperCase();
  const url = config.url ?? '';
  const started = config.metadata?.startedAt;
  const ms = started != null ? Math.round(performance.now() - started) : undefined;
  const mutation = ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method);
  const payload = { method, url, status, durationMs: ms };
  if (mutation) {
    appLogger.info('ApiClient', 'responseOk', `${method} ${url} concluído`, payload);
  } else {
    appLogger.debug('ApiClient', 'responseOk', `${method} ${url} concluído`, payload);
  }
}

api.interceptors.request.use((config) => {
  const c = attachTiming(config);
  const token = typeof window !== 'undefined' ? localStorage.getItem(STORAGE_CUSTOMER_TOKEN) : null;
  if (token) {
    const headers = c.headers ?? {};
    headers.Authorization = `Bearer ${token}`;
    c.headers = headers;
  }
  return c;
});

adminApi.interceptors.request.use((config) => {
  const c = attachTiming(config);
  const headers = c.headers ?? {};
  const token = typeof window !== 'undefined' ? localStorage.getItem(STORAGE_ADMIN_TOKEN) : null;
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (import.meta.env.VITE_ADMIN_KEY) {
    headers['X-Admin-Key'] = import.meta.env.VITE_ADMIN_KEY;
  }
  c.headers = headers;
  return c;
});

api.interceptors.response.use(
  (r) => {
    logSuccessResponse(r.config as ConfigWithMeta, r.status);
    return r;
  },
  (err: AxiosError<unknown>) => rejectWithParsedError(err),
);

adminApi.interceptors.response.use(
  (r) => {
    logSuccessResponse(r.config as ConfigWithMeta, r.status);
    return r;
  },
  (err: AxiosError<unknown>) => rejectWithParsedError(err, { admin403: true }),
);

/**
 * Mensagem legível para exibir ao usuário. Cobre `Error`, respostas Axios já
 * transformadas em `Error`, e o objeto serializado que o `.unwrap()` do RTK lança.
 */
export function getErrorMessage(e: unknown): string {
  if (e == null) return '';
  if (typeof e === 'string') return e;
  if (e instanceof Error) return e.message;
  if (typeof e === 'object') {
    const o = e as Record<string, unknown>;
    if (typeof o.message === 'string' && o.message.trim()) return o.message;
    if (typeof o.detail === 'string' && o.detail.trim()) return o.detail;
  }
  try {
    return JSON.stringify(e);
  } catch {
    return String(e);
  }
}
