import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { useAppSelector } from '@/app/hooks';
import { MainLayout } from '@/layouts/MainLayout';
import { AuthLayout } from '@/layouts/AuthLayout';
import { ProfileLayout } from '@/layouts/ProfileLayout';
import { AdminLayout } from '@/layouts/AdminLayout';
import { HomePage } from '@/pages/HomePage';
import { BooksPage } from '@/pages/BooksPage';
import { BookDetailPage } from '@/pages/BookDetailPage';
import { CartPage } from '@/pages/CartPage';
import { CheckoutPage } from '@/pages/CheckoutPage';
import { LoginPage } from '@/pages/LoginPage';
import { RegisterPage } from '@/pages/RegisterPage';
import { ProfileOverviewPage } from '@/pages/profile/ProfileOverviewPage';
import { ProfileOrdersPage } from '@/pages/profile/ProfileOrdersPage';
import { ProfileAddressesPage } from '@/pages/profile/ProfileAddressesPage';
import { ProfileCardsPage } from '@/pages/profile/ProfileCardsPage';
import { ProfileTransactionsPage } from '@/pages/profile/ProfileTransactionsPage';
import { AdminAnalyticsPage } from '@/pages/AdminAnalyticsPage';
import { AdminOrdersPage } from '@/pages/AdminOrdersPage';
import { AdminExchangesPage } from '@/pages/AdminExchangesPage';
import { AdminInventoryPage } from '@/pages/AdminInventoryPage';
import { AdminAuditPage } from '@/pages/AdminAuditPage';
import { AdminBooksPage } from '@/pages/AdminBooksPage';
import { AdminCustomersPage } from '@/pages/AdminCustomersPage';
import { AdminCouponsPage } from '@/pages/AdminCouponsPage';
import { AiChatPage } from '@/pages/AiChatPage';
import { STORAGE_CUSTOMER_ID } from '@/constants/storageKeys';
import { ROUTES } from '@/constants/routes';
import { hasValidAdminSession } from '@/utils/adminSession';

function Protected({ children }: { children: React.ReactNode }) {
  const id = useAppSelector((s) => s.auth.customerId);
  const status = useAppSelector((s) => s.auth.status);
  const user = useAppSelector((s) => s.auth.user);

  if (status === 'loading') {
    return (
      <div className="flex min-h-[40vh] items-center justify-center text-sm text-ink-muted">Carregando sessão…</div>
    );
  }
  if (!id) return <Navigate to="/login" replace />;
  if (user && !user.active) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

function ProtectedAdmin({ children }: { children: React.ReactNode }) {
  if (hasValidAdminSession()) return <>{children}</>;
  if (typeof window !== 'undefined' && localStorage.getItem(STORAGE_CUSTOMER_ID)) {
    return <Navigate to={ROUTES.home} replace />;
  }
  return <Navigate to={ROUTES.loginWithAdminTab} replace />;
}

export function AppRoutes() {
  return (
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Routes>
        <Route element={<MainLayout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/books" element={<BooksPage />} />
          <Route path="/books/:id" element={<BookDetailPage />} />
          <Route
            path="/cart"
            element={
              <Protected>
                <CartPage />
              </Protected>
            }
          />
          <Route
            path="/checkout"
            element={
              <Protected>
                <CheckoutPage />
              </Protected>
            }
          />
          <Route path="/ai/chat" element={<AiChatPage />} />
        </Route>

        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/admin/login" element={<Navigate to={ROUTES.loginWithAdminTab} replace />} />
        </Route>

        <Route
          element={
            <Protected>
              <ProfileLayout />
            </Protected>
          }
        >
          <Route path="/profile" element={<ProfileOverviewPage />} />
          <Route path="/profile/orders" element={<ProfileOrdersPage />} />
          <Route path="/profile/transactions" element={<ProfileTransactionsPage />} />
          <Route path="/profile/addresses" element={<ProfileAddressesPage />} />
          <Route path="/profile/cards" element={<ProfileCardsPage />} />
        </Route>

        <Route
          path="/admin"
          element={
            <ProtectedAdmin>
              <AdminLayout />
            </ProtectedAdmin>
          }
        >
          <Route index element={<Navigate to="analytics" replace />} />
          <Route path="analytics" element={<AdminAnalyticsPage />} />
          <Route path="orders" element={<AdminOrdersPage />} />
          <Route path="exchanges" element={<AdminExchangesPage />} />
          <Route path="inventory" element={<AdminInventoryPage />} />
          <Route path="audit" element={<AdminAuditPage />} />
          <Route path="books" element={<AdminBooksPage />} />
          <Route path="customers" element={<AdminCustomersPage />} />
          <Route path="coupons" element={<AdminCouponsPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
