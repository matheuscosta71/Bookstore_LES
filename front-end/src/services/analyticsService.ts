import { adminApi } from './api';
import type { SalesLineChart } from '@/types/api';

export type SalesSummaryDto = {
  totalRevenue: number;
  totalItemsSold: number;
  orderCount: number;
};

export type BookSalesRow = {
  bookId: string;
  title: string;
  revenue: number;
  quantitySold: number;
};

export type CategorySalesRow = {
  category: string;
  revenue: number;
  quantitySold: number;
};

export async function fetchSalesSummary(
  startDate: string,
  endDate: string,
): Promise<SalesSummaryDto> {
  const { data } = await adminApi.get<SalesSummaryDto>('/analytics/sales-history', {
    params: { startDate, endDate },
  });
  return data;
}

export async function fetchSalesLineChart(
  startDate: string,
  endDate: string,
): Promise<SalesLineChart> {
  const { data } = await adminApi.get<SalesLineChart>('/analytics/sales-history/line-chart', {
    params: { startDate, endDate },
  });
  return data;
}

export async function fetchSalesByBooks(
  startDate: string,
  endDate: string,
): Promise<{ books: BookSalesRow[] }> {
  const { data } = await adminApi.get<{ books: BookSalesRow[] }>('/analytics/sales-history/books', {
    params: { startDate, endDate },
  });
  return data;
}

export async function fetchSalesByCategories(
  startDate: string,
  endDate: string,
): Promise<{ categories: CategorySalesRow[] }> {
  const { data } = await adminApi.get<{ categories: CategorySalesRow[] }>(
    '/analytics/sales-history/categories',
    {
      params: { startDate, endDate },
    },
  );
  return { categories: data.categories ?? [] };
}
