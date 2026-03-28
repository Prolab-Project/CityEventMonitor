import { apiClient, API_BASE } from './client';
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

export async function getMapNews(params: NewsQueryParams): Promise<News[]> {
  const response = await apiClient.get<News[]>('/news/map', {
    params,
  });
  return response.data;
}

export async function getFilters(): Promise<Filters> {
  const response = await apiClient.get<Filters>('/news/filters');
  return response.data;
}

/** POST /news/scrape yanıtı */
export interface ScrapeResult {
  totalScraped: number;
  newSaved: number;
  duplicatesMerged: number;
  geocodingFailed: number;
  scrapedBySource: Record<string, number>;
}

/** GET /news/scrape/stream SSE olayları */
export interface ScrapeProgressEvent {
  phase: 'SOURCE_START' | 'SOURCE_DONE' | 'COMPLETE';
  sourceName?: string;
  sourceIndex?: number;
  sourceTotal?: number;
  extractedCount?: number;
  summary?: ScrapeResult;
}

/**
 * SSE ile sıralı scrape — her kaynak için canlı olay (UI ilerleme çubuğu için).
 */
export function triggerScrapeStream(
  days: number,
  onProgress: (e: ScrapeProgressEvent) => void,
): Promise<ScrapeResult> {
  return new Promise((resolve, reject) => {
    const url = `${API_BASE}/news/scrape/stream?days=${encodeURIComponent(String(days))}`;
    let settled = false;
    const es = new EventSource(url);

    const fail = (err: Error) => {
      if (settled) return;
      settled = true;
      es.close();
      reject(err);
    };

    es.addEventListener('scrape', (ev: MessageEvent) => {
      try {
        const data = JSON.parse(ev.data as string) as ScrapeProgressEvent;
        onProgress(data);
        if (data.phase === 'COMPLETE' && data.summary) {
          settled = true;
          es.close();
          resolve(data.summary);
        }
      } catch (e) {
        fail(e instanceof Error ? e : new Error(String(e)));
      }
    });

    es.addEventListener('scrape_error', (ev: MessageEvent) => {
      try {
        const payload = JSON.parse(ev.data as string) as { message?: string };
        fail(new Error(payload.message ?? 'Scrape hatası'));
      } catch {
        fail(new Error('Scrape hatası'));
      }
    });

    es.onerror = () => {
      if (settled) return;
      fail(new Error('SSE bağlantısı kesildi veya sunucuya ulaşılamadı.'));
    };
  });
}

/**
 * Tüm kaynaklardan scraping — backend senkron çalışır ve 5 site + detay sayfaları nedeniyle
 * dakikalar sürebilir. Varsayılan axios timeout'unu aşmamak için uzun süre verilir.
 */
export async function triggerScrape(days = 3): Promise<ScrapeResult> {
  const response = await apiClient.post<ScrapeResult>(`/news/scrape`, null, {
    params: { days },
    timeout: 900_000,
  });
  return response.data;
}

