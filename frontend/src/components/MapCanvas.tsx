import { useMemo, useState } from 'react';
import { GoogleMap, InfoWindow, Marker, useJsApiLoader } from '@react-google-maps/api';
import type { News, NewsType } from '../types/news';

interface MapCanvasProps {
  news: News[];
}

interface DisplayNews extends News {
  displayLat: number;
  displayLng: number;
}

const KOCAELI_CENTER = { lat: 40.765, lng: 29.94 };

const containerStyle: React.CSSProperties = {
  width: '100%',
  height: '100%',
};

const MARKER_COLORS: Record<NewsType, string> = {
  TRAFIK_KAZASI: '#dc2626',
  YANGIN: '#ea580c',
  ELEKTRIK_KESINTISI: '#ca8a04',
  HIRSIZLIK: '#7c3aed',
  KULTUREL_ETKINLIK: '#16a34a',
};

const DEFAULT_MARKER_COLOR = '#6b7280';

function getMarkerIcon(type: NewsType | null): google.maps.Symbol {
  const fillColor = type ? MARKER_COLORS[type] : DEFAULT_MARKER_COLOR;
  return {
    path: google.maps.SymbolPath.CIRCLE,
    fillColor,
    fillOpacity: 1,
    strokeColor: '#1f2937',
    strokeWeight: 1.5,
    scale: 10,
  };
}

export default function MapCanvas({ news }: MapCanvasProps) {
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string | undefined;

  const { isLoaded, loadError } = useJsApiLoader({
    id: 'google-maps-script',
    googleMapsApiKey: apiKey ?? '',
  });

  const [selectedId, setSelectedId] = useState<string | null>(null);

  const markers = useMemo<DisplayNews[]>(() => {
    const valid = news.filter(
      (n) =>
        !n.geocodingFailed &&
        !!n.district &&
        n.district.trim().length > 0 &&
        typeof n.latitude === 'number' &&
        typeof n.longitude === 'number' &&
        (n.latitude !== 0 || n.longitude !== 0),
    );

    const byCoordinate = new Map<string, News[]>();
    for (const n of valid) {
      const key = `${n.latitude.toFixed(6)},${n.longitude.toFixed(6)}`;
      const arr = byCoordinate.get(key) ?? [];
      arr.push(n);
      byCoordinate.set(key, arr);
    }

    const result: DisplayNews[] = [];
    for (const group of byCoordinate.values()) {
      if (group.length === 1) {
        const n = group[0];
        result.push({ ...n, displayLat: n.latitude, displayLng: n.longitude });
        continue;
      }

      const radius = 0.00035;
      group.forEach((n, i) => {
        const angle = (2 * Math.PI * i) / group.length;
        result.push({
          ...n,
          displayLat: n.latitude + radius * Math.sin(angle),
          displayLng: n.longitude + radius * Math.cos(angle),
        });
      });
    }
    return result;
  }, [news]);

  if (!apiKey) {
    return <p>Google Maps API anahtarı tanımlı değil. Lütfen VITE_GOOGLE_MAPS_API_KEY ayarlayın.</p>;
  }
  if (loadError) {
    return <p>Harita yüklenirken hata oluştu.</p>;
  }
  if (!isLoaded) {
    return <p>Harita yükleniyor...</p>;
  }

  const selectedNews = markers.find((n) => n.id === selectedId) ?? null;

  return (
    <GoogleMap
      mapContainerStyle={containerStyle}
      center={KOCAELI_CENTER}
      zoom={11}
      options={{
        disableDefaultUI: true,
        zoomControl: true,
        streetViewControl: false,
        mapTypeControl: false,
      }}
    >
      {markers.map((n) => (
        <Marker
          key={n.id}
          position={{ lat: n.displayLat, lng: n.displayLng }}
          icon={getMarkerIcon(n.type)}
          onClick={() => setSelectedId(n.id)}
        />
      ))}

      {selectedNews && (
        <InfoWindow
          position={{ lat: selectedNews.displayLat, lng: selectedNews.displayLng }}
          onCloseClick={() => setSelectedId(null)}
        >
          <div className="map-infowindow">
            <h3>{selectedNews.title}</h3>
            <p>{selectedNews.district ?? 'İlçe bilgisi yok'}</p>
            {selectedNews.locationText && (
              <p>
                <strong>Konum:</strong> {selectedNews.locationText}
              </p>
            )}
            <p>
              {new Date(selectedNews.publishDate).toLocaleString('tr-TR', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              })}
            </p>
            {selectedNews.sources.length > 0 && (
              <p>
                <strong>Kaynak:</strong> {selectedNews.sources.join(', ')}
              </p>
            )}
            {selectedNews.urls.length > 0 && (
              <p className="map-infowindow-links">
                {selectedNews.urls.map((url, i) => (
                  <a key={i} href={url} target="_blank" rel="noreferrer">
                    Habere git{selectedNews.urls.length > 1 ? ` (${i + 1})` : ''}
                  </a>
                ))}
              </p>
            )}
          </div>
        </InfoWindow>
      )}
    </GoogleMap>
  );
}
