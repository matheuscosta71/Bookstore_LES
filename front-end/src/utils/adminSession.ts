import { STORAGE_ADMIN_ROLE, STORAGE_ADMIN_TOKEN } from '@/constants/storageKeys';

/** Sessão admin válida para exibir atalho e acessar /admin (token + role). */
export function hasValidAdminSession(): boolean {
  if (typeof window === 'undefined') return false;
  const token = localStorage.getItem(STORAGE_ADMIN_TOKEN);
  const role = localStorage.getItem(STORAGE_ADMIN_ROLE);
  return Boolean(token && role === 'ADMIN');
}
