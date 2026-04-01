export type NewsType =
  | 'TRAFIK_KAZASI'
  | 'YANGIN'
  | 'ELEKTRIK_KESINTISI'
  | 'HIRSIZLIK'
  | 'KULTUREL_ETKINLIK'
  | 'DIGER';

export interface News {
  id: string;
  title: string;
  content: string;
  type: NewsType | null;
  district: string | null;
  locationText: string | null;
  latitude: number;
  longitude: number;
  geocodingFailed: boolean;
  sources: string[];
  urls: string[];
  publishDate: string; // ISO date string
}

