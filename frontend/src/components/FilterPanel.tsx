import type { Filters } from '../types/filters';
import type { NewsType } from '../types/news';
import { useMemo, useState } from 'react';

export interface FilterState {
  type?: NewsType | '';
  district?: string;
  startDate?: string;
  endDate?: string;
  search?: string;
}

interface FilterPanelProps {
  filters: Filters | null;
  value: FilterState;
  onChange: (next: FilterState) => void;
  onSubmit: () => void;
}

const typeLabels: Record<NewsType, string> = {
  TRAFIK_KAZASI: 'Trafik Kazası',
  YANGIN: 'Yangın',
  ELEKTRIK_KESINTISI: 'Elektrik Kesintisi',
  HIRSIZLIK: 'Hırsızlık',
  KULTUREL_ETKINLIK: 'Kültürel Etkinlik',
};

export function FilterPanel({ filters, value, onChange, onSubmit }: FilterPanelProps) {
  const [localState, setLocalState] = useState<FilterState>(value);

  const handleFieldChange = <K extends keyof FilterState>(key: K, val: FilterState[K]) => {
    const next = { ...localState, [key]: val };
    setLocalState(next);
    onChange(next);
  };

  const districts = useMemo(
    () => (filters?.districts ?? []).filter((d) => d && d.trim().length > 0),
    [filters],
  );

  const types = filters?.types ?? [];

  return (
    <div className="filter-panel-inner">
      <div className="filter-field">
        <label htmlFor="type">Haber Türü</label>
        <select
          id="type"
          value={localState.type ?? ''}
          onChange={(e) => handleFieldChange('type', e.target.value as NewsType | '')}
        >
          <option value="">Tümü</option>
          {types.map((t) => (
            <option key={t} value={t}>
              {typeLabels[t] ?? t}
            </option>
          ))}
        </select>
      </div>

      <div className="filter-field">
        <label htmlFor="district">İlçe</label>
        <select
          id="district"
          value={localState.district ?? ''}
          onChange={(e) => handleFieldChange('district', e.target.value || undefined)}
        >
          <option value="">Tümü</option>
          {districts.map((d) => (
            <option key={d} value={d}>
              {d}
            </option>
          ))}
        </select>
      </div>

      <div className="filter-field filter-field-row">
        <div>
          <label htmlFor="startDate">Başlangıç Tarihi</label>
          <input
            id="startDate"
            type="date"
            value={localState.startDate ?? ''}
            onChange={(e) => handleFieldChange('startDate', e.target.value || undefined)}
          />
        </div>
        <div>
          <label htmlFor="endDate">Bitiş Tarihi</label>
          <input
            id="endDate"
            type="date"
            value={localState.endDate ?? ''}
            onChange={(e) => handleFieldChange('endDate', e.target.value || undefined)}
          />
        </div>
      </div>

      <div className="filter-field">
        <label htmlFor="search">Arama</label>
        <input
          id="search"
          type="text"
          placeholder="Başlık ve içerikte ara..."
          value={localState.search ?? ''}
          onChange={(e) => handleFieldChange('search', e.target.value || undefined)}
        />
      </div>

      <button className="filter-submit" type="button" onClick={onSubmit}>
        Filtrele
      </button>
    </div>
  );
}

export default FilterPanel;

