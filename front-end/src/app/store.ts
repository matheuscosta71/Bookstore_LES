import { configureStore } from '@reduxjs/toolkit';
import authReducer from '@/features/auth/authSlice';
import booksReducer from '@/features/books/booksSlice';
import cartReducer from '@/features/cart/cartSlice';
import checkoutReducer from '@/features/checkout/checkoutSlice';
import customerReducer from '@/features/customer/customerSlice';
import aiReducer from '@/features/ai/aiSlice';
import analyticsReducer from '@/features/analytics/analyticsSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    books: booksReducer,
    cart: cartReducer,
    checkout: checkoutReducer,
    customer: customerReducer,
    ai: aiReducer,
    analytics: analyticsReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
