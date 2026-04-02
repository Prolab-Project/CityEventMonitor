import './App.css';
import { useState, useEffect, useCallback } from 'react';
import dayjs from 'dayjs';
import 'dayjs/locale/tr';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { trTR } from '@mui/x-date-pickers/locales';

dayjs.locale('tr');
import type { Filters } from './types/filters';
import type { News } from './types/news';
import type { PagedResponse } from './types/paged-response';
import type { FilterState } from './components/FilterPanel';
import FilterPanel from './components/FilterPanel';
import NewsList from './components/NewsList';
import MapCanvas from './components/MapCanvas';
import NewsFetchStatsBar from './components/NewsFetchStatsBar';
import {
  getFilters,
  getMapNews,
  getNews,
  triggerScrapeStream,
  deleteAllNews,
  type ScrapeResult,
} from './api/newsApi';
import { 
  CircularProgress, 
  Alert, 
  Box, 
  Backdrop,
  ThemeProvider,
  createTheme
} from '@mui/material';

const theme = createTheme({
  palette: {
    mode: 'dark', // Keeping it dark by default as the original CSS is dark
    primary: {
      main: '#22c55e',
    },
  },
  typography: {
    // Varsayılan MUI "Roboto" ailesi Google Fonts isteği tetikleyebilir; sistem fontu = ek ağ yok
    fontFamily:
      'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
  },
});

function App() {
  // Metadata for filter panel
  const [filtersMetadata, setFiltersMetadata] = useState<Filters | null>(null);
  
  // Form state
  const [filterState, setFilterState] = useState<FilterState>({});
  
  // Applied filters for the query
  const [appliedFilters, setAppliedFilters] = useState<FilterState>({});

  const [newsPage, setNewsPage] = useState<PagedResponse<News> | null>(null);
  const [mapNews, setMapNews] = useState<News[]>([]);
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  
  const [isLoading, setIsLoading] = useState(false);
  /** Uzun süren /news/scrape; tam ekran Backdrop ile karıştırılmaz */
  const [scrapeLoading, setScrapeLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastScrape, setLastScrape] = useState<ScrapeResult | null>(null);
  const [lastScrapeAt, setLastScrapeAt] = useState<number | null>(null);
  const [totalInDatabase, setTotalInDatabase] = useState<number | null>(null);
  /** SSE ile anlık kaynak sırası (gösterge çubuğu) */
  const [scrapeLive, setScrapeLive] = useState<{
    currentSource: string | null;
    currentIndex: number;
    totalSources: number;
    completed: { sourceName: string; extractedCount: number }[];
  } | null>(null);

  // Fetch filter metadata on mount
  useEffect(() => {
    getFilters()
      .then(setFiltersMetadata)
      .catch((err) => {
        console.error('Error fetching filters:', err);
      });
  }, []);

  // Backend ISO datetime bekliyor (YYYY-MM-DDTHH:mm:ss); date input sadece YYYY-MM-DD veriyor
  const toStartOfDay = (dateStr: string | undefined) =>
    dateStr ? `${dateStr}T00:00:00` : undefined;
  const toEndOfDay = (dateStr: string | undefined) =>
    dateStr ? `${dateStr}T23:59:59` : undefined;

  // Main data fetching logic
  const fetchNews = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [response, dbHead] = await Promise.all([
        getNews({
          type: appliedFilters.type || undefined,
          district: appliedFilters.district || undefined,
          startDate: toStartOfDay(appliedFilters.startDate),
          endDate: toEndOfDay(appliedFilters.endDate),
          search: appliedFilters.search || undefined,
          page,
          size,
        }),
        getNews({ page: 0, size: 1 }),
      ]);
      setNewsPage(response);
      setTotalInDatabase(dbHead.totalElements);

      // Harita sayfalamadan bağımsız çalışmalı; filtreye uyan tüm haberleri al.
      const mapItems = await getMapNews({
        type: appliedFilters.type || undefined,
        district: appliedFilters.district || undefined,
        startDate: toStartOfDay(appliedFilters.startDate),
        endDate: toEndOfDay(appliedFilters.endDate),
        search: appliedFilters.search || undefined,
      });
      setMapNews(mapItems);
    } catch (err: any) {
      console.error('Error fetching news:', err);
      setError('Haberler yüklenirken bir hata oluştu. Lütfen tekrar deneyin.');
    } finally {
      setIsLoading(false);
    }
  }, [appliedFilters, page, size]);

  useEffect(() => {
    fetchNews();
  }, [fetchNews]);

  const handleRefreshAllNews = async () => {
    setError(null);
    setScrapeLoading(true);
    setScrapeLive({
      currentSource: null,
      currentIndex: 0,
      totalSources: 0,
      completed: [],
    });
    try {
      const scrapeResult = await triggerScrapeStream(3, (ev) => {
        if (ev.phase === 'SOURCE_START' && ev.sourceName && ev.sourceIndex && ev.sourceTotal) {
          setScrapeLive((prev) => ({
            currentSource: ev.sourceName!,
            currentIndex: ev.sourceIndex!,
            totalSources: ev.sourceTotal!,
            completed: prev?.completed ?? [],
          }));
          return;
        }
        if (ev.phase === 'SOURCE_DONE' && ev.sourceName != null && ev.sourceIndex && ev.sourceTotal) {
          const count = ev.extractedCount ?? 0;
          setScrapeLive((prev) => ({
            currentSource: null,
            currentIndex: ev.sourceIndex!,
            totalSources: ev.sourceTotal!,
            completed: [
              ...(prev?.completed ?? []),
              { sourceName: ev.sourceName!, extractedCount: count },
            ],
          }));
        }
      });
      setLastScrape(scrapeResult);
      setLastScrapeAt(Date.now());
      const bySource = scrapeResult.scrapedBySource ?? {};
      console.log(
        '[Scrape] Özet — toplam çekilen (ham):',
        scrapeResult.totalScraped,
        '| yeni:',
        scrapeResult.newSaved,
        '| mükerrer:',
        scrapeResult.duplicatesMerged,
        '| geocoding hata:',
        scrapeResult.geocodingFailed,
      );
      console.log('[Scrape] Kaynak bazlı çekilen haber sayıları:');
      console.table(bySource);
    } catch (err: any) {
      console.error('Error refreshing news:', err);
      setError('Haberleri yeniden çekme sırasında bir hata oluştu. Lütfen tekrar deneyin.');
      return;
    } finally {
      setScrapeLoading(false);
      setScrapeLive(null);
    }
    await fetchNews();
  };

  const handleClearDatabase = async () => {
    if (!window.confirm("Bütün veritabanını silmek istediğinize emin misiniz? Bu işlem geri alınamaz!")) return;
    
    setIsLoading(true);
    try {
      const res = await deleteAllNews();
      alert(`Sıfırlama başarılı!\nSilinen haber sayısı: ${res.deletedCount}`);
      await fetchNews();
    } catch (err) {
      console.error(err);
      alert("Sıfırlama işlemi başarısız oldu.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleFilterChange = (next: FilterState) => {
    setFilterState(next);
  };

  const handleFilterSubmit = () => {
    setPage(0);
    setAppliedFilters({ ...filterState });
  };

  const handlePageChange = (nextPage: number) => {
    setPage(nextPage);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <ThemeProvider theme={theme}>
      <LocalizationProvider
        dateAdapter={AdapterDayjs}
        adapterLocale="tr"
        localeText={trTR.components.MuiLocalizationProvider.defaultProps.localeText}
      >
      <div className="app-root">
        <header className="app-header">
          <h1>City Event Monitor</h1>
          <span className="app-subtitle">Kocaeli kentsel olay haritası</span>
        </header>

        <main className="app-main">
          <section className="filters-panel">
            <h2>Filtreler</h2>
            <FilterPanel
              filters={filtersMetadata}
              value={filterState}
              onChange={handleFilterChange}
              onSubmit={handleFilterSubmit}
            />
            <div className="filter-scrape-section">
              <p className="filter-scrape-label">Veri kaynakları</p>
              <p className="filter-scrape-hint">
                Beş haber sitesinden son günlerin içeriğini çeker; işlem birkaç dakika sürebilir.
              </p>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                <button
                  type="button"
                className="filter-scrape-btn"
                disabled={isLoading || scrapeLoading}
                onClick={handleRefreshAllNews}
              >
                {scrapeLoading ? (
                  <>
                    <CircularProgress size={20} thickness={5} sx={{ color: '#6ee7b7' }} />
                    <span>Kaynaklar çekiliyor…</span>
                  </>
                ) : (
                  <>
                    <span className="filter-scrape-btn__icon" aria-hidden>
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path
                          d="M4 4v6h6M20 20v-6h-6"
                          stroke="currentColor"
                          strokeWidth="2"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                        <path
                          d="M5 19a9 9 0 0 0 14.65-3.5M19 5a9 9 0 0 0-14.65 3.5"
                          stroke="currentColor"
                          strokeWidth="2"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                      </svg>
                    </span>
                    <span className="filter-scrape-btn__text">Haberleri kaynaklardan yenile</span>
                  </>
                )}
              </button>
              
              <button
                type="button"
                className="filter-scrape-btn"
                style={{ backgroundColor: 'rgba(239, 68, 68, 0.1)', color: '#ef4444', borderColor: '#ef4444' }}
                disabled={isLoading || scrapeLoading}
                onClick={handleClearDatabase}
              >
                Tüm Veritabanını Sıfırla
              </button>
              </div>
            </div>
            <NewsFetchStatsBar
              filteredTotal={newsPage?.totalElements ?? 0}
              totalInDatabase={totalInDatabase}
              lastScrape={lastScrape}
              lastScrapeAt={lastScrapeAt}
              scrapeLoading={scrapeLoading}
              scrapeLive={scrapeLive}
            />
          </section>

          <section className="content-panel">
            {scrapeLoading && (
              <Box mb={2}>
                <Alert severity="info">
                  Kaynak sitelerden veri alınıyor (birkaç dakika sürebilir). Bu sırada liste ve harita
                  kullanılabilir; işlem bitince güncellenir.
                </Alert>
              </Box>
            )}
            {error && (
              <Box mb={2}>
                <Alert severity="error" onClose={() => setError(null)}>
                  {error}
                </Alert>
              </Box>
            )}
            
            <div className="content-grid">
              <div className="map-container">
                <h2>Harita</h2>
                <MapCanvas news={mapNews} />
              </div>

              <div className="list-container">
                <h2>Haber Listesi</h2>
                <NewsList
                  items={newsPage?.items ?? []}
                  totalElements={newsPage?.totalElements ?? 0}
                  page={page}
                  size={size}
                  onPageChange={handlePageChange}
                />
              </div>
            </div>
          </section>
        </main>

        <Backdrop
          sx={{ color: '#fff', zIndex: (theme) => theme.zIndex.drawer + 1 }}
          open={isLoading}
        >
          <CircularProgress color="inherit" />
        </Backdrop>
      </div>
      </LocalizationProvider>
    </ThemeProvider>
  );
}

export default App;

