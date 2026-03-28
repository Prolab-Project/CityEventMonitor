import type { ScrapeResult } from '../api/newsApi';
import { Box, LinearProgress, Typography } from '@mui/material';

const SOURCE_COLORS = ['#22c55e', '#38bdf8', '#a78bfa', '#fb923c', '#f472b6'];

export interface ScrapeLiveState {
  currentSource: string | null;
  currentIndex: number;
  totalSources: number;
  completed: { sourceName: string; extractedCount: number }[];
}

export interface NewsFetchStatsBarProps {
  /** Filtreye uyan kayıt sayısı (liste ile aynı) */
  filteredTotal: number;
  /** Filtre olmadan veritabanındaki toplam haber */
  totalInDatabase: number | null;
  lastScrape: ScrapeResult | null;
  lastScrapeAt: number | null;
  scrapeLoading: boolean;
  /** SSE ile anlık sıra (çekiliyor / bu kaynak bitti) */
  scrapeLive: ScrapeLiveState | null;
}

function formatTime(ts: number | null) {
  if (ts == null) return null;
  try {
    return new Intl.DateTimeFormat('tr-TR', {
      dateStyle: 'short',
      timeStyle: 'medium',
    }).format(new Date(ts));
  } catch {
    return null;
  }
}

export default function NewsFetchStatsBar({
  filteredTotal,
  totalInDatabase,
  lastScrape,
  lastScrapeAt,
  scrapeLoading,
  scrapeLive,
}: NewsFetchStatsBarProps) {
  const bySource = lastScrape?.scrapedBySource ?? {};
  const entries = Object.entries(bySource).filter(([, n]) => n > 0);
  const totalRaw = lastScrape?.totalScraped ?? 0;
  const timeLabel = formatTime(lastScrapeAt);

  return (
    <div className="news-fetch-stats-bar">
      {scrapeLoading && (
        <LinearProgress
          color="primary"
          sx={{ position: 'absolute', top: 0, left: 0, right: 0, height: 3, borderRadius: '20px 20px 0 0' }}
        />
      )}

      {scrapeLoading && scrapeLive && (
        <div className="news-fetch-stats-bar__live">
          <Typography variant="caption" color="primary.main" fontWeight={600} display="block" sx={{ mb: 0.75 }}>
            Canlı çekim sırası
          </Typography>
          {scrapeLive.completed.length > 0 && (
            <ul className="news-fetch-stats-bar__live-done">
              {scrapeLive.completed.map((row, idx) => (
                <li key={`${idx}-${row.sourceName}`}>
                  <span className="news-fetch-stats-bar__live-check">✓</span>
                  <strong>{row.sourceName}</strong>
                  <span className="news-fetch-stats-bar__live-muted">
                    — bu kaynaktan <strong>{row.extractedCount}</strong> ham haber işlendi
                  </span>
                </li>
              ))}
            </ul>
          )}
          {scrapeLive.currentSource ? (
            <Typography variant="body2" className="news-fetch-stats-bar__live-current">
              Şu an: <strong>{scrapeLive.currentSource}</strong> — haberler çekiliyor (
              {scrapeLive.currentIndex}/{scrapeLive.totalSources || '?'})
            </Typography>
          ) : scrapeLive.totalSources > 0 && scrapeLive.completed.length < scrapeLive.totalSources ? (
            <Typography variant="caption" color="text.secondary">
              Sonraki kaynağa geçiliyor…
            </Typography>
          ) : scrapeLive.totalSources === 0 ? (
            <Typography variant="caption" color="text.secondary">
              Kaynaklar sırayla başlatılıyor…
            </Typography>
          ) : null}
        </div>
      )}

      <div className="news-fetch-stats-bar__row news-fetch-stats-bar__row--live">
        <Typography variant="caption" color="text.secondary" component="span">
          Veritabanı toplam
        </Typography>
        <Typography variant="body2" fontWeight={700} color="primary.main" component="span">
          {totalInDatabase != null ? totalInDatabase.toLocaleString('tr-TR') : '—'}
        </Typography>
        <span className="news-fetch-stats-bar__sep" aria-hidden />
        <Typography variant="caption" color="text.secondary" component="span">
          Filtreye uyan
        </Typography>
        <Typography variant="body2" fontWeight={600} component="span">
          {filteredTotal.toLocaleString('tr-TR')}
        </Typography>
      </div>

      {lastScrape && (
        <>
          <div className="news-fetch-stats-bar__row news-fetch-stats-bar__meta">
            <Typography variant="caption" color="text.secondary">
              Son çekimde işlenen (ham):{' '}
              <strong>{totalRaw.toLocaleString('tr-TR')}</strong>
              {' · '}
              Yeni: {lastScrape.newSaved.toLocaleString('tr-TR')} · Mükerrer:{' '}
              {lastScrape.duplicatesMerged.toLocaleString('tr-TR')}
              {lastScrape.geocodingFailed > 0 && (
                <> · Konum hatası: {lastScrape.geocodingFailed}</>
              )}
            </Typography>
            {timeLabel && (
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.25 }}>
                {timeLabel}
              </Typography>
            )}
          </div>

          {totalRaw > 0 && (
            <>
              <Box
                className="news-fetch-stats-bar__track"
                role="img"
                aria-label="Kaynaklara göre son çekimde işlenen ham haber oranı"
              >
                {entries.map(([name, count], i) => (
                  <Box
                    key={name}
                    className="news-fetch-stats-bar__segment"
                    sx={{
                      width: `${(count / totalRaw) * 100}%`,
                      backgroundColor: SOURCE_COLORS[i % SOURCE_COLORS.length],
                    }}
                    title={`${name}: ${count}`}
                  />
                ))}
              </Box>

              <ul className="news-fetch-stats-bar__legend">
                {Object.entries(bySource).map(([name, count], i) => (
                  <li key={name}>
                    <span
                      className="news-fetch-stats-bar__dot"
                      style={{ backgroundColor: SOURCE_COLORS[i % SOURCE_COLORS.length] }}
                    />
                    <span className="news-fetch-stats-bar__legend-name">{name}</span>
                    <span className="news-fetch-stats-bar__legend-count">{count}</span>
                  </li>
                ))}
              </ul>
            </>
          )}
        </>
      )}

      {!lastScrape && !scrapeLoading && (
        <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5 }}>
          Kaynak dağılımını görmek için üstteki &quot;Haberleri Yeniden Çek&quot; ile çalıştırın.
        </Typography>
      )}
    </div>
  );
}
