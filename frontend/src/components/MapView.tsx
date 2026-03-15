import { useMemo, useState } from 'react';
import { GoogleMap, InfoWindow, Marker, useJsApiLoader } from '@react-google-maps/api';
import type { News, NewsType } from '../types/news';

interface MapViewProps {
  news: News[];
}

const KOCAELI_CENTER = { lat: 40.765, lng: 29.940 }; // yaklaşık Kocaeli merkezi

const containerStyle: React.CSSProperties = {
  width: '100%',
  height: '100%',
};

/** Haber türüne göre marker rengi (proje şartnamesi: marker rengi ve sembolü türe göre farklı olmalı) */
const MARKER_COLORS: Record<NewsType, string> = {
  TRAFIK_KAZASI: '#dc2626',   // kırmızı
  YANGIN: '#ea580c',           // turuncu
  ELEKTRIK_KESINTISI: '#ca8a04', // sarı/amber
  HIRSIZLIK: '#7c3aed',       // mor
  KULTUREL_ETKINLIK: '#16a34a',  // yeşil
};

const DEFAULT_MARKER_COLOR = '#6b7280'; // tür yoksa gri

/** Haber türüne göre marker ikonu (Google Maps Symbol: daire, renk türe göre) */
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

export function MapView({ news }: MapViewProps) {
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string | undefined;

  const { isLoaded, loadError } = useJsApiLoader({
    id: 'google-maps-script',
    googleMapsApiKey: apiKey ?? '',
  });

  const [selectedId, setSelectedId] = useState<string | null>(null);

  const markers = useMemo(
    () =>
      news.filter(
        (n) =>
          !n.geocodingFailed &&
          typeof n.latitude === 'number' &&
          typeof n.longitude === 'number' &&
          (n.latitude !== 0 || n.longitude !== 0),
      ),
    [news],
  );

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
          position={{ lat: n.latitude, lng: n.longitude }}
          icon={getMarkerIcon(n.type)}
          onClick={() => setSelectedId(n.id)}
        />
      ))}

      {selectedNews && (
        <InfoWindow
          position={{ lat: selectedNews.latitude, lng: selectedNews.longitude }}
          onCloseClick={() => setSelectedId(null)}
        >
          <div className="map-infowindow">
            <h3>{selectedNews.title}</h3>
            <p>{selectedNews.district ?? 'İlçe bilgisi yok'}</p>
            <p>{new Date(selectedNews.publishDate).toLocaleString('tr-TR', {
              day: '2-digit',
              month: '2-digit',
              year: 'numeric',
              hour: '2-digit',
              minute: '2-digit',
            })}</p>
            <p>Kaynak sayısı: {selectedNews.sources.length}</p>
            {selectedNews.urls[0] && (
              <a href={selectedNews.urls[0]} target="_blank" rel="noreferrer">
                Habere git
              </a>
            )}
          </div>
        </InfoWindow>
      )}
    </GoogleMap>
  );
}

export default MapView;

