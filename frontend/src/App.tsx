import './App.css';
import { useState } from 'react';
import type { Filters } from './types/filters';
import type { News } from './types/news';
import type { PagedResponse } from './types/paged-response';
import type { FilterState } from './components/FilterPanel';
import FilterPanel from './components/FilterPanel';
import NewsList from './components/NewsList';
import MapView from './components/MapView';

function App() {
  const [filters, setFilters] = useState<Filters | null>(null);
  const [filterState, setFilterState] = useState<FilterState>({});

  const [newsPage, setNewsPage] = useState<PagedResponse<News> | null>(null);
  const [page, setPage] = useState(0);
  const [size] = useState(20);

  const handleFilterChange = (next: FilterState) => {
    setFilterState(next);
  };

  const handleFilterSubmit = () => {
    // Sonraki adımda buradan getNews() çağıracağız.
    console.log('Filtre gönderildi:', filterState, 'page:', page, 'size:', size);
  };

  const handlePageChange = (nextPage: number) => {
    setPage(nextPage);
    // Sonraki adımda sayfa değişince de getNews() çağıracağız.
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
          <FilterPanel filters={filters} value={filterState} onChange={handleFilterChange} onSubmit={handleFilterSubmit} />
        </section>

        <section className="content-panel">
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
        </section>
      </main>
    </div>
  );
}

export default App;

