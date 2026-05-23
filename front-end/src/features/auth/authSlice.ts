import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import type { Customer } from '@/types/api';
import { STORAGE_CUSTOMER_ID } from '@/constants/storageKeys';
import * as authService from '@/services/authService';
import { appLogger } from '@/utils/appLogger';

type AuthState = {
  customerId: string | null;
  user: Customer | null;
  status: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
};

function readStoredCustomerId(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(STORAGE_CUSTOMER_ID);
}

const storedCustomerId = readStoredCustomerId();
const initialState: AuthState = {
  customerId: storedCustomerId,
  user: null,
  status: storedCustomerId ? 'loading' : 'idle',
  error: null,
};

export const hydrateAuth = createAsyncThunk('auth/hydrate', async () => {
  const id = localStorage.getItem(STORAGE_CUSTOMER_ID);
  if (!id) return null;
  const customer = await authService.fetchCustomer(id);
  if (!customer.active) {
    authService.clearCustomerSessionStorage();
    return null;
  }
  return customer;
});

export const registerUser = createAsyncThunk(
  'auth/register',
  async (payload: Parameters<typeof authService.registerCustomer>[0]) => {
    return authService.registerCustomer(payload);
  },
);

export const loginUser = createAsyncThunk(
  'auth/login',
  async ({ email, password }: { email: string; password: string }) => {
    await authService.loginCustomer(email, password);
    const id = localStorage.getItem(STORAGE_CUSTOMER_ID);
    if (!id) throw new Error('Sessão não iniciada.');
    return authService.fetchCustomer(id);
  },
);

export const loadProfile = createAsyncThunk(
  'auth/loadProfile',
  async (customerId: string, { rejectWithValue }) => {
    const customer = await authService.fetchCustomer(customerId);
    if (!customer.active) {
      authService.clearCustomerSessionStorage();
      return rejectWithValue('silent');
    }
    return customer;
  },
);

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    logout(state) {
      const id = state.customerId;
      state.user = null;
      state.customerId = null;
      state.error = null;
      state.status = 'idle';
      authService.clearCustomerSessionStorage();
      if (id) {
        appLogger.info('authSlice', 'logout', 'Sessão encerrada', { customerId: id });
      }
    },
    clearError(state) {
      state.error = null;
    },
  },
  extraReducers: (b) => {
    b.addCase(hydrateAuth.pending, (s) => {
      if (s.customerId) s.status = 'loading';
    });
    b.addCase(hydrateAuth.fulfilled, (s, a) => {
      if (a.payload) {
        s.status = 'succeeded';
        s.user = a.payload;
        s.customerId = a.payload.id;
      } else {
        s.status = 'idle';
        s.customerId = null;
        s.user = null;
      }
    });
    b.addCase(hydrateAuth.rejected, (s) => {
      s.status = 'idle';
      s.customerId = null;
      s.user = null;
      authService.clearCustomerSessionStorage();
      s.error = null;
    });

    b.addCase(registerUser.pending, (s) => {
      s.status = 'loading';
      s.error = null;
    });
    b.addCase(registerUser.fulfilled, (s) => {
      s.status = 'idle';
      s.error = null;
      appLogger.info('authSlice', 'registerUser', 'Cadastro concluído (sem login automático)');
    });
    b.addCase(registerUser.rejected, (s, a) => {
      s.status = 'failed';
      s.error = a.error.message ?? 'Erro ao cadastrar';
      appLogger.warn('authSlice', 'registerUser', 'Cadastro falhou', {
        message: s.error,
      });
    });

    b.addCase(loginUser.pending, (s) => {
      s.status = 'loading';
      s.error = null;
    });
    b.addCase(loginUser.fulfilled, (s, a) => {
      s.status = 'succeeded';
      s.user = a.payload;
      s.customerId = a.payload.id;
      appLogger.info('authSlice', 'loginUser', 'Login concluído', { customerId: a.payload.id });
    });
    b.addCase(loginUser.rejected, (s, a) => {
      s.status = 'failed';
      s.error = a.error.message ?? 'Erro ao entrar';
      // #region agent log
      fetch('http://127.0.0.1:7449/ingest/fe3f462e-f2fd-41a9-818d-3105cdb7b029', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Debug-Session-Id': '4b6485' },
        body: JSON.stringify({
          sessionId: '4b6485',
          hypothesisId: 'H2',
          location: 'authSlice.ts:loginUser.rejected',
          message: 'loginUser rejected',
          data: { stateErrorPrefix: s.error?.slice(0, 120), errName: a.error.name },
          timestamp: Date.now(),
        }),
      }).catch(() => {});
      // #endregion
      appLogger.warn('authSlice', 'loginUser', 'Login falhou', { message: s.error });
    });

    b.addCase(loadProfile.fulfilled, (s, a) => {
      s.user = a.payload;
      s.customerId = a.payload.id;
    });
    b.addCase(loadProfile.rejected, (s, a) => {
      if (a.payload === 'silent') {
        s.user = null;
        s.customerId = null;
        s.status = 'idle';
        s.error = null;
      }
    });
  },
});

export const { logout, clearError } = authSlice.actions;
export default authSlice.reducer;

export const selectAuth = (s: { auth: AuthState }) => s.auth;
export const selectCustomerId = (s: { auth: AuthState }) => s.auth.customerId;
