import { apiClient } from './client';
import type { News } from '../types/news';
import type { PagedResponse } from '../types/paged-response';
import type { Filters } from '../types/filters';

export interface NewsQueryParams {
  type?: string;
  district?: string;
  startDate?: string;
  endDate?: string;
  search?: string;
  page?: number;
  size?: number;
}

export async function getNews(params: NewsQueryParams): Promise<PagedResponse<News>> {
  const response = await apiClient.get<PagedResponse<News>>('/news', {
    params,
  });
  return response.data;
}

export async function getFilters(): Promise<Filters> {
  const response = await apiClient.get<Filters>('/news/filters');
  return response.data;
}

export async function triggerScrape(days = 3): Promise<void> {
  await apiClient.post(`/news/scrape`, null, {
    params: { days },
  });
}

