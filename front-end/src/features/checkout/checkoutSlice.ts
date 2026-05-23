import { createAsyncThunk, createSlice, type PayloadAction } from '@reduxjs/toolkit';
import type { RootState } from '@/app/store';
import type { FreightResponse, Order } from '@/types/api';
import * as checkoutService from '@/services/checkoutService';
import { appLogger } from '@/utils/appLogger';

type CheckoutState = {
  freight: FreightResponse | null;
  lastOrder: Order | null;
  selectedAddressId: string | null;
  status: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
};

const initialState: CheckoutState = {
  freight: null,
  lastOrder: null,
  selectedAddressId: null,
  status: 'idle',
  error: null,
};

export const calculateFreight = createAsyncThunk(
  'checkout/freight',
  async ({ customerId, addressId }: { customerId: string; addressId: string }) => {
    return checkoutService.postFreight(customerId, addressId);
  },
);

export const applyAddress = createAsyncThunk(
  'checkout/address',
  async (arg: { customerId: string; body: checkoutService.CheckoutAddressBody }) => {
    await checkoutService.postCheckoutAddress(arg.customerId, arg.body);
  },
);

export const applyPayment = createAsyncThunk(
  'checkout/payment',
  async (arg: { customerId: string; body: checkoutService.CheckoutPaymentBody }) => {
    await checkoutService.postCheckoutPayment(arg.customerId, arg.body);
  },
);

export const finalizeOrder = createAsyncThunk(
  'checkout/finalize',
  async (customerId: string, { getState }) => {
    // #region agent log
    const state = getState() as RootState;
    const lines = (state.cart.cart?.items ?? []).map((i) => ({
      bookId: i.bookId,
      qty: i.quantity,
    }));
    fetch('http://127.0.0.1:7297/ingest/c6a8463e-416b-4c93-9bc4-166b78a1d291', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Debug-Session-Id': 'd25ba1' },
      body: JSON.stringify({
        sessionId: 'd25ba1',
        hypothesisId: 'H2',
        location: 'checkoutSlice.finalizeOrder',
        message: 'redux_cart_before_finalize',
        data: { customerId, lines },
        timestamp: Date.now(),
      }),
    }).catch(() => {});
    // #endregion
    return checkoutService.finalizeCheckout(customerId);
  },
);

const checkoutSlice = createSlice({
  name: 'checkout',
  initialState,
  reducers: {
    setSelectedAddressId(state, a: PayloadAction<string | null>) {
      state.selectedAddressId = a.payload;
    },
    resetCheckout(state) {
      state.freight = null;
      state.lastOrder = null;
      state.error = null;
      state.status = 'idle';
    },
    clearCheckoutError(state) {
      state.error = null;
    },
  },
  extraReducers: (b) => {
    b.addCase(calculateFreight.fulfilled, (s, a) => {
      s.freight = a.payload;
      const arg = a.meta.arg as { customerId: string; addressId: string };
      appLogger.info('checkoutSlice', 'calculateFreight', 'Frete calculado', {
        customerId: arg.customerId,
        grandTotal: a.payload.grandTotal,
      });
    });
    b.addCase(calculateFreight.rejected, (_s, a) => {
      appLogger.warn('checkoutSlice', 'calculateFreight', 'Falha ao calcular frete', {
        message: a.error.message,
      });
    });
    b.addCase(applyAddress.rejected, (_s, a) => {
      appLogger.warn('checkoutSlice', 'applyAddress', 'Falha ao aplicar endereço', {
        message: a.error.message,
      });
    });
    b.addCase(applyPayment.rejected, (_s, a) => {
      appLogger.warn('checkoutSlice', 'applyPayment', 'Falha ao aplicar pagamento', {
        message: a.error.message,
      });
    });
    b.addCase(finalizeOrder.pending, (s) => {
      s.status = 'loading';
      s.error = null;
    });
    b.addCase(finalizeOrder.fulfilled, (s, a) => {
      s.status = 'succeeded';
      s.lastOrder = a.payload;
      appLogger.info('checkoutSlice', 'finalizeOrder', 'Pedido finalizado', {
        orderId: a.payload.id,
        status: a.payload.status,
        totalAmount: a.payload.totalAmount,
      });
    });
    b.addCase(finalizeOrder.rejected, (s, a) => {
      s.status = 'failed';
      s.error = a.error.message ?? 'Não foi possível finalizar';
      appLogger.warn('checkoutSlice', 'finalizeOrder', 'Finalização falhou', { message: s.error });
    });
  },
});

export const { setSelectedAddressId, resetCheckout, clearCheckoutError } = checkoutSlice.actions;
export default checkoutSlice.reducer;

export const selectCheckout = (s: { checkout: CheckoutState }) => s.checkout;
