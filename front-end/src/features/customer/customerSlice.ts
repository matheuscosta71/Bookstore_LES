import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import type { Address, CreditCard, Customer, Order } from '@/types/api';
import { logout } from '@/features/auth/authSlice';
import * as authService from '@/services/authService';
import * as customerService from '@/services/customerService';

type CustomerState = {
  profile: Customer | null;
  addresses: Address[];
  cards: CreditCard[];
  orders: Order[];
  transactions: customerService.TransactionRow[];
  status: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
};

const initialCustomerState: CustomerState = {
  profile: null,
  addresses: [],
  cards: [],
  orders: [],
  transactions: [],
  status: 'idle',
  error: null,
};

export const fetchProfileBundle = createAsyncThunk(
  'customer/fetchBundle',
  async (customerId: string, { dispatch, rejectWithValue }) => {
    const [profile, addresses, cards, orders, transactions] = await Promise.all([
      authService.fetchCustomer(customerId),
      customerService.listAddresses(customerId),
      customerService.listCards(customerId),
      customerService.listOrders(customerId),
      customerService.listTransactions(customerId),
    ]);
    if (!profile.active) {
      authService.clearCustomerSessionStorage();
      dispatch(logout());
      return rejectWithValue('inactive');
    }
    return { profile, addresses, cards, orders, transactions };
  },
);

export const updateProfile = createAsyncThunk(
  'customer/update',
  async (arg: { id: string; body: Parameters<typeof customerService.updateCustomer>[1] }) => {
    return customerService.updateCustomer(arg.id, arg.body);
  },
);

export const fetchCustomerOrders = createAsyncThunk('customer/orders', async (customerId: string) => {
  return customerService.listOrders(customerId);
});

/** Atualiza pedidos e extrato (ex.: após finalizar compra — RF0025). */
export const fetchOrdersAndTransactions = createAsyncThunk(
  'customer/ordersAndTransactions',
  async (customerId: string) => {
    const [orders, transactions] = await Promise.all([
      customerService.listOrders(customerId),
      customerService.listTransactions(customerId),
    ]);
    return { orders, transactions };
  },
);

const customerSlice = createSlice({
  name: 'customer',
  initialState: initialCustomerState,
  reducers: {
    clearCustomerError(state) {
      state.error = null;
    },
    resetCustomerState() {
      return { ...initialCustomerState };
    },
  },
  extraReducers: (b) => {
    b.addCase(logout, () => ({ ...initialCustomerState }));
    b.addCase(fetchProfileBundle.pending, (s) => {
      s.status = 'loading';
      s.error = null;
    });
    b.addCase(fetchProfileBundle.fulfilled, (s, a) => {
      s.status = 'succeeded';
      s.profile = a.payload.profile;
      s.addresses = a.payload.addresses;
      s.cards = a.payload.cards;
      s.orders = a.payload.orders;
      s.transactions = a.payload.transactions;
    });
    b.addCase(fetchProfileBundle.rejected, (s, a) => {
      if (a.payload === 'inactive') {
        s.profile = null;
        s.addresses = [];
        s.cards = [];
        s.orders = [];
        s.transactions = [];
        s.status = 'idle';
        s.error = null;
        return;
      }
      s.status = 'failed';
      s.error = a.error.message ?? 'Erro ao carregar perfil';
    });

    b.addCase(updateProfile.fulfilled, (s, a) => {
      s.profile = a.payload;
    });

    b.addCase(fetchCustomerOrders.fulfilled, (s, a) => {
      s.orders = a.payload;
    });

    b.addCase(fetchOrdersAndTransactions.fulfilled, (s, a) => {
      s.orders = a.payload.orders;
      s.transactions = a.payload.transactions;
    });
  },
});

export const { clearCustomerError, resetCustomerState } = customerSlice.actions;
export default customerSlice.reducer;

export const selectCustomerState = (s: { customer: CustomerState }) => s.customer;
