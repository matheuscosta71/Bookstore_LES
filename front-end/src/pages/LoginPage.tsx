import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useNavigate, useLocation, useSearchParams, Navigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/app/hooks';
import { loginUser, clearError } from '@/features/auth/authSlice';
import { loginSchema } from '@/utils/schemas';
import { PasswordField } from '@/components/PasswordField';
import { ROUTES } from '@/constants/routes';
import { loginAdmin } from '@/services/authService';
import { getErrorMessage } from '@/services/api';
import { hasValidAdminSession } from '@/utils/adminSession';
import { cn } from '@/utils/cn';
import { z } from 'zod';
import { useEffect, useCallback, useState } from 'react';

type LocationState = { registered?: boolean };

const TAB_QUERY = 'tab';
const TAB_ADMIN = 'admin';

export function LoginPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const isAdminTab = searchParams.get(TAB_QUERY) === TAB_ADMIN;

  const err = useAppSelector((s) => s.auth.error);
  const status = useAppSelector((s) => s.auth.status);
  const state = location.state as LocationState | null;
  const successMsg =
    state?.registered === true ? 'Conta criada com sucesso. Faça login.' : null;

  const [adminUsername, setAdminUsername] = useState('admin');
  const [adminPassword, setAdminPassword] = useState('admin');
  const [adminLoading, setAdminLoading] = useState(false);
  const [adminErr, setAdminErr] = useState<string | null>(null);

  const { register, handleSubmit, formState: { errors } } = useForm<z.infer<typeof loginSchema>>({
    resolver: zodResolver(loginSchema),
  });

  const setTab = useCallback(
    (admin: boolean) => {
      if (admin) {
        setSearchParams({ [TAB_QUERY]: TAB_ADMIN }, { replace: true });
      } else {
        setSearchParams({}, { replace: true });
      }
    },
    [setSearchParams],
  );

  useEffect(() => {
    dispatch(clearError());
    setAdminErr(null);
  }, [dispatch, isAdminTab]);

  if (isAdminTab && hasValidAdminSession()) {
    return <Navigate to={ROUTES.adminAnalytics} replace />;
  }

  return (
    <div>
      <div
        className="flex rounded-xl border border-stone-200 bg-stone-50/80 p-1"
        role="tablist"
        aria-label="Tipo de entrada"
      >
        <button
          type="button"
          role="tab"
          aria-selected={!isAdminTab}
          className={cn(
            'flex-1 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
            !isAdminTab ? 'bg-white text-brand shadow-sm' : 'text-ink-muted hover:text-ink',
          )}
          onClick={() => setTab(false)}
        >
          Cliente
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={isAdminTab}
          className={cn(
            'flex-1 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
            isAdminTab ? 'bg-white text-brand shadow-sm' : 'text-ink-muted hover:text-ink',
          )}
          onClick={() => setTab(true)}
        >
          Admin
        </button>
      </div>

      {!isAdminTab ? (
        <>
          <h1 className="mt-6 font-display text-2xl font-semibold text-ink">Entrar</h1>
          <p className="mt-1 text-sm text-ink-muted">
            Use o e-mail e a senha cadastrados. A autenticação é validada no servidor.
          </p>
          {successMsg && (
            <p className="mt-3 rounded-lg border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-900" role="status">
              {successMsg}
            </p>
          )}
          <form
            className="mt-6 space-y-4"
            onSubmit={handleSubmit(async (data) => {
              dispatch(clearError());
              try {
                await dispatch(loginUser(data)).unwrap();
                navigate(ROUTES.home);
              } catch {
                // Mensagem já em auth.error (rejected do loginUser)
              }
            })}
          >
            <div>
              <label className="text-sm font-medium">E-mail</label>
              <input
                type="email"
                {...register('email')}
                className="mt-1 w-full rounded-lg border border-stone-200 px-3 py-2 text-sm"
              />
              {errors.email && <p className="mt-1 text-xs text-red-600">{errors.email.message}</p>}
            </div>
            <label className="block">
              <span className="text-sm font-medium">Senha</span>
              <div className="mt-1">
                <PasswordField {...register('password')} autoComplete="current-password" />
              </div>
              {errors.password && <p className="mt-1 text-xs text-red-600">{errors.password.message}</p>}
            </label>
            {err && <p className="text-sm text-red-600">{err}</p>}
            <button
              type="submit"
              disabled={status === 'loading'}
              className="w-full rounded-xl bg-brand py-3 text-sm font-semibold text-white hover:bg-brand-light disabled:opacity-50"
            >
              {status === 'loading' ? 'Entrando…' : 'Entrar'}
            </button>
          </form>
          <p className="mt-4 text-center text-sm text-ink-muted">
            Não tem conta?{' '}
            <Link to={ROUTES.register} className="font-medium text-brand">
              Cadastre-se
            </Link>
          </p>
        </>
      ) : (
        <>
          <h1 className="mt-6 font-display text-2xl font-semibold text-ink">Área administrativa</h1>
          <p className="mt-1 text-sm text-ink-muted">Usuário e senha de administrador.</p>
          <form
            className="mt-6 space-y-4"
            onSubmit={async (e) => {
              e.preventDefault();
              setAdminErr(null);
              setAdminLoading(true);
              try {
                await loginAdmin(adminUsername.trim(), adminPassword);
                navigate(ROUTES.adminAnalytics, { replace: true });
              } catch (err) {
                const msg = getErrorMessage(err);
                if (msg.includes('401')) {
                  setAdminErr('Usuário ou senha inválidos.');
                } else {
                  setAdminErr(msg || 'Falha no login admin.');
                }
              } finally {
                setAdminLoading(false);
              }
            }}
          >
            <div>
              <label className="text-sm font-medium">Usuário</label>
              <input
                value={adminUsername}
                onChange={(e) => setAdminUsername(e.target.value)}
                className="mt-1 w-full rounded-lg border border-stone-200 px-3 py-2 text-sm"
                autoComplete="username"
              />
            </div>
            <label className="block">
              <span className="text-sm font-medium">Senha</span>
              <div className="mt-1">
                <PasswordField
                  value={adminPassword}
                  onChange={(e) => setAdminPassword(e.target.value)}
                  autoComplete="current-password"
                />
              </div>
            </label>
            {adminErr && <p className="text-sm text-red-600">{adminErr}</p>}
            <button
              type="submit"
              disabled={adminLoading || !adminUsername.trim() || !adminPassword}
              className="w-full rounded-xl bg-brand py-3 text-sm font-semibold text-white hover:bg-brand-light disabled:opacity-50"
            >
              {adminLoading ? 'Entrando…' : 'Entrar no admin'}
            </button>
          </form>
          <p className="mt-4 text-center text-sm text-ink-muted">
            <Link to={ROUTES.home} className="font-medium text-brand">
              Voltar à loja
            </Link>
          </p>
        </>
      )}
    </div>
  );
}
