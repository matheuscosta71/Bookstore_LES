import { StrictMode, useEffect } from 'react';
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
import { store } from '@/app/store';
import { hydrateAuth } from '@/features/auth/authSlice';
import { AppRoutes } from '@/routes/AppRoutes';
import '@/styles/index.css';

function Bootstrap() {
  useEffect(() => {
    store.dispatch(hydrateAuth());
  }, []);
  return (
    <Provider store={store}>
      <AppRoutes />
    </Provider>
  );
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Bootstrap />
  </StrictMode>,
);
