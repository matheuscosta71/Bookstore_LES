import { Navigate } from 'react-router-dom';
import { ROUTES } from '@/constants/routes';

/** Legado: use `/login?tab=admin` ou a aba Admin em `/login`. */
export function AdminLoginPage() {
  return <Navigate to={ROUTES.loginWithAdminTab} replace />;
}
