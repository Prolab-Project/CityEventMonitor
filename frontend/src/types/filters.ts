import type { NewsType } from './news';

export interface Filters {
  types: NewsType[];
  districts: string[];
  minPublishDate: string | null; // ISO date string
  maxPublishDate: string | null; // ISO date string
}

