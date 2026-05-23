import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import type { Cart } from '@/types/api';
import * as cartService from '@/services/cartService';
import { appLogger } from '@/utils/appLogger';
import { clearReservationNotice, saveReservationNotice } from '@/utils/reservationNoticeStorage';

type CartState = {
  cart: Cart | null;
  status: 'idle' | 'loading' | 'succeeded' | 'failed';
  mutationStatus: 'idle' | 'loading';
  error: string | null;
};

const initialState: CartState = {
  cart: null,
  status: 'idle',
  mutationStatus: 'idle',
  error: null,
};

export const loadCart = createAsyncThunk('cart/load', async (customerId: string) => {
  return cartService.getCart(customerId);
});

export const addToCart = createAsyncThunk(
  'cart/add',
  async ({ customerId, bookId, quantity }: { customerId: string; bookId: string; quantity: number }) => {
    return cartService.addCartItem(customerId, { bookId, quantity });
  },
);

export const updateCartLine = createAsyncThunk(
  'cart/update',
  async (arg: {
    customerId: string;
    itemId: string;
    bookId: string;
    quantity: number;
  }) => {
    const { customerId, itemId, bookId, quantity } = arg;
    return cartService.updateCartItem(customerId, itemId, { bookId, quantity });
  },
);

export const removeFromCart = createAsyncThunk(
  'cart/remove',
  async ({ customerId, itemId }: { customerId: string; itemId: string }) => {
    await cartService.removeCartItem(customerId, itemId);
    return cartService.getCart(customerId);
  },
);

const cartSlice = createSlice({
  name: 'cart',
  initialState,
  reducers: {
    clearCartError(state) {
      state.error = null;
    },
    resetCart(state) {
      state.cart = null;
      state.status = 'idle';
      state.mutationStatus = 'idle';
      state.error = null;
    },
  },
  extraReducers: (b) => {
    const pendingLoad = (s: CartState) => {
      s.status = 'loading';
      s.error = null;
    };
    const fail = (s: CartState, msg: string | undefined) => {
      s.status = 'failed';
      s.error = msg ?? 'Erro no carrinho';
    };

    b.addCase(loadCart.pending, pendingLoad);
    b.addCase(loadCart.fulfilled, (s, a) => {
      s.status = 'succeeded';
      s.cart = a.payload;
      const customerId = a.meta.arg as string;
      const msgs = a.payload.reservationExpiredMessages;
      if (msgs && msgs.length > 0) {
        saveReservationNotice(customerId, {
          messages: msgs,
          itemExpirationMinutes: a.payload.itemExpirationMinutes,
        });
      }
    });
    b.addCase(loadCart.rejected, (s, a) => fail(s, a.error.message));

    const mutPending = (s: CartState) => {
      s.mutationStatus = 'loading';
      s.error = null;
    };
    const mutDone = (s: CartState, a: { payload: Cart }) => {
      s.mutationStatus = 'idle';
      s.cart = a.payload;
      s.status = 'succeeded';
    };

    b.addCase(addToCart.pending, mutPending);
    b.addCase(addToCart.fulfilled, (s, a) => {
      mutDone(s, a);
      const arg = a.meta.arg as { customerId: string; bookId: string; quantity: number };
      clearReservationNotice(arg.customerId);
      appLogger.info('cartSlice', 'addToCart', 'Item adicionado ao carrinho', {
        customerId: arg.customerId,
        bookId: arg.bookId,
        quantity: arg.quantity,
      });
    });
    b.addCase(addToCart.rejected, (s, a) => {
      s.mutationStatus = 'idle';
      fail(s, a.error.message);
      appLogger.warn('cartSlice', 'addToCart', 'Falha ao adicionar item', { message: s.error });
    });

    b.addCase(updateCartLine.pending, mutPending);
    b.addCase(updateCartLine.fulfilled, mutDone);
    b.addCase(updateCartLine.rejected, (s, a) => {
      s.mutationStatus = 'idle';
      fail(s, a.error.message);
    });

    b.addCase(removeFromCart.pending, mutPending);
    b.addCase(removeFromCart.fulfilled, (s, a) => {
      s.mutationStatus = 'idle';
      s.cart = a.payload;
      const arg = a.meta.arg as { customerId: string; itemId: string };
      appLogger.info('cartSlice', 'removeFromCart', 'Item removido', {
        customerId: arg.customerId,
        itemId: arg.itemId,
      });
    });
    b.addCase(removeFromCart.rejected, (s, a) => {
      s.mutationStatus = 'idle';
      fail(s, a.error.message);
      appLogger.warn('cartSlice', 'removeFromCart', 'Falha ao remover item', { message: s.error });
    });
  },
});

export const { clearCartError, resetCart } = cartSlice.actions;
export default cartSlice.reducer;

export const selectCartState = (s: { cart: CartState }) => s.cart;
