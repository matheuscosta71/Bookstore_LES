import { createAsyncThunk, createSlice, type PayloadAction } from '@reduxjs/toolkit';
import type { SalesLineChart, SalesCategoryVolumeChart } from '@/types/api';
import * as analyticsService from '@/services/analyticsService';

type AnalyticsState = {
  startDate: string;
  endDate: string;
  summary: analyticsService.SalesSummaryDto | null;
  lineChart: SalesLineChart | null;
  categoryVolumeChart: SalesCategoryVolumeChart | null;
  books: analyticsService.BookSalesRow[];
  categories: analyticsService.CategorySalesRow[];
  status: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
};

function defaultRange() {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - 30);
  return {
    startDate: start.toISOString().slice(0, 10),
    endDate: end.toISOString().slice(0, 10),
  };
}

const dr = defaultRange();

const initialState: AnalyticsState = {
  startDate: dr.startDate,
  endDate: dr.endDate,
  summary: null,
  lineChart: null,
  categoryVolumeChart: null,
  books: [],
  categories: [],
  status: 'idle',
  error: null,
};

export const loadAnalyticsDashboard = createAsyncThunk(
  'analytics/load',
  async (arg: { startDate: string; endDate: string }) => {
    const [summary, lineChart, categoryVolumeChart, booksRes, catRes] = await Promise.all([
      analyticsService.fetchSalesSummary(arg.startDate, arg.endDate),
      analyticsService.fetchSalesLineChart(arg.startDate, arg.endDate),
      analyticsService.fetchSalesCategoryVolumeChart(arg.startDate, arg.endDate),
      analyticsService.fetchSalesByBooks(arg.startDate, arg.endDate),
      analyticsService.fetchSalesByCategories(arg.startDate, arg.endDate),
    ]);
    return {
      summary,
      lineChart,
      categoryVolumeChart,
      books: booksRes.books ?? [],
      categories: catRes.categories ?? [],
    };
  },
);

const analyticsSlice = createSlice({
  name: 'analytics',
  initialState,
  reducers: {
    setDateRange(state, a: PayloadAction<{ startDate: string; endDate: string }>) {
      state.startDate = a.payload.startDate;
      state.endDate = a.payload.endDate;
    },
  },
  extraReducers: (b) => {
    b.addCase(loadAnalyticsDashboard.pending, (s) => {
      s.status = 'loading';
      s.error = null;
    });
    b.addCase(loadAnalyticsDashboard.fulfilled, (s, a) => {
      s.status = 'succeeded';
      s.summary = a.payload.summary;
      s.lineChart = a.payload.lineChart;
      s.categoryVolumeChart = a.payload.categoryVolumeChart;
      s.books = a.payload.books;
      s.categories = a.payload.categories;
    });
    b.addCase(loadAnalyticsDashboard.rejected, (s, e) => {
      s.status = 'failed';
      s.error = e.error.message ?? 'Erro ao carregar analytics';
    });
  },
});

export const { setDateRange } = analyticsSlice.actions;
export default analyticsSlice.reducer;

export const selectAnalytics = (s: { analytics: AnalyticsState }) => s.analytics;
