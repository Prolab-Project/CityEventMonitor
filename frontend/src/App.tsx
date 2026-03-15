import './App.css';
import { useState, useEffect, useCallback } from 'react';
import type { Filters } from './types/filters';
import type { News } from './types/news';
import type { PagedResponse } from './types/paged-response';
import type { FilterState } from './components/FilterPanel';
import FilterPanel from './components/FilterPanel';
import NewsList from './components/NewsList';
import MapView from './components/MapView';
import { getFilters, getNews } from './api/newsApi';
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
});

function App() {
  // Metadata for filter panel
  const [filtersMetadata, setFiltersMetadata] = useState<Filters | null>(null);
  
  // Form state
  const [filterState, setFilterState] = useState<FilterState>({});
  
  // Applied filters for the query
  const [appliedFilters, setAppliedFilters] = useState<FilterState>({});

  const [newsPage, setNewsPage] = useState<PagedResponse<News> | null>(null);
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
      const response = await getNews({
        type: appliedFilters.type || undefined,
        district: appliedFilters.district || undefined,
        startDate: toStartOfDay(appliedFilters.startDate),
        endDate: toEndOfDay(appliedFilters.endDate),
        search: appliedFilters.search || undefined,
        page,
        size,
      });
      setNewsPage(response);
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
          </section>

          <section className="content-panel">
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
                <MapView news={newsPage?.items ?? []} />
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
    </ThemeProvider>
  );
}

export default App;

