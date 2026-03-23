import type { News } from '../types/news';

interface NewsListProps {
  items: News[];
  totalElements: number;
  page: number;
  size: number;
  onPageChange: (nextPage: number) => void;
}

const formatDateTime = (iso: string | undefined) => {
  if (!iso) return '-';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('tr-TR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export function NewsList({ items, totalElements, page, size, onPageChange }: NewsListProps) {
  const totalPages = Math.max(1, Math.ceil(totalElements / (size || 1)));

  const canPrev = page > 0;
  const canNext = page + 1 < totalPages;

  const handlePrev = () => {
    if (canPrev) onPageChange(page - 1);
  };

  const handleNext = () => {
    if (canNext) onPageChange(page + 1);
  };

  return (
    <div className="news-list-root">
      {items.length === 0 ? (
        <p className="news-list-empty">Bu filtrelerde haber bulunamadı.</p>
      ) : (
        <ul className="news-list">
          {items.map((n) => (
            <li key={n.id} className="news-list-item">
              <div className="news-list-main">
                <a
                  href={n.urls[0] ?? '#'}
                  target="_blank"
                  rel="noreferrer"
                  className="news-list-title"
                >
                  {n.title}
                </a>
                {n.locationText && (
                  <div className="news-list-location">
                    Konum: {n.locationText}
                  </div>
                )}
                <div className="news-list-meta">
                  <span>{n.district ?? 'İlçe bilgisi yok'}</span>
                  <span>{n.type ?? 'Tür bilinmiyor'}</span>
                  <span>{formatDateTime(n.publishDate)}</span>
                  <span>Kaynak sayısı: {n.sources?.length ?? 0}</span>
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}

      <div className="news-list-pagination">
        <button type="button" onClick={handlePrev} disabled={!canPrev}>
          Önceki
        </button>
        <span>
          Sayfa {page + 1} / {totalPages}
        </span>
        <button type="button" onClick={handleNext} disabled={!canNext}>
          Sonraki
        </button>
      </div>
    </div>
  );
}

export default NewsList;

