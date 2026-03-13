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

  // Main data fetching logic
  const fetchNews = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await getNews({
        type: appliedFilters.type || undefined,
        district: appliedFilters.district || undefined,
        startDate: appliedFilters.startDate || undefined,
        endDate: appliedFilters.endDate || undefined,
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
          {error && <div className="error-message">{error}</div>}
          
          <div className="map-container">
            <h2>Harita</h2>
            <MapView news={newsPage?.items ?? []} />
          </div>

          <div className="list-container">
            <h2>Haber Listesi {isLoading && <span className="loading-spinner">...</span>}</h2>
            <NewsList
              items={newsPage?.items ?? []}
              totalElements={newsPage?.totalElements ?? 0}
              page={page}
              size={size}
              onPageChange={handlePageChange}
            />
          </div>
        </section>
      </main>
    </div>
  );
}

export default App;

