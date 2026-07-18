# City Event Monitor

Kocaeli yerel haberlerini toplayan, sınıflandıran ve harita üzerinde görselleştiren web uygulaması (Yazılım Laboratuvarı II – Proje 1).

- **Backend:** Java 17, Spring Boot, MongoDB
- **Frontend:** React, TypeScript, Vite, Google Maps

---

## Gereksinimler

- JDK 17+
- Node.js 18+
- MongoDB (yerel veya uzak)
- Google Cloud hesabı (Geocoding API ve Maps JavaScript API için)

---

## API Anahtarlarını Ayarlama (Zorunlu)

Projenin çalışması için aşağıdaki API anahtarlarının güvenli şekilde ayarlanması gerekir. **Anahtarları asla doğrudan kod veya `application.properties` içine yazmayın.**

### 1. Backend – Geocoding API

**Seçenek A – Ortam değişkeni (önerilen)**

```bash
export GEOCODING_API_KEY=your_google_geocoding_api_key_here
```

Windows (PowerShell):

```powershell
$env:GEOCODING_API_KEY="your_google_geocoding_api_key_here"
```

**Seçenek B – Yerel dosya (git’e eklenmez)**

1. Proje kökünde `application-local.properties` oluşturun (örnek için `application-local.properties.example` dosyasına bakın).
2. İçine ekleyin:

   ```properties
   geocoding.api-key=your_google_geocoding_api_key_here
   ```

`application-local.properties` dosyası `.gitignore` ile takip dışındadır.

### 2. Frontend – Google Maps

Haritanın çalışması için Maps JavaScript API anahtarı gerekir.

1. `frontend` klasöründe `.env` veya `.env.local` oluşturun (bu dosyalar git’e eklenmemelidir).
2. Şunu ekleyin:

   ```
   VITE_GOOGLE_MAPS_API_KEY=your_google_maps_javascript_api_key_here
   ```

Geliştirme sunucusunu yeniden başlatın (`npm run dev`).

---

## Çalıştırma

### MongoDB

MongoDB’nin çalıştığından emin olun (varsayılan: `mongodb://localhost:27017`). Veritabanı adı: `newsdb`.

### Backend

```bash
./mvnw spring-boot:run
```

API: `http://localhost:8080`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Arayüz: `http://localhost:5173` (veya Vite’ın gösterdiği adres)

---

## Özet – Ayarlanacak Değişkenler

| Amaç              | Yöntem                          | Değişken / Dosya                    |
|-------------------|----------------------------------|-------------------------------------|
| Backend Geocoding | Ortam değişkeni                 | `GEOCODING_API_KEY`                 |
| Backend Geocoding | Yerel properties (opsiyonel)    | `application-local.properties`      |
| Frontend Harita   | `.env` / `.env.local`           | `VITE_GOOGLE_MAPS_API_KEY`          |

Bu anahtarlar ayarlanmadan Geocoding ve harita özellikleri çalışmaz.

## Örnek Görüntüler

<img width="1329" height="691" alt="image" src="https://github.com/user-attachments/assets/aac57033-834b-454b-bb42-5d8d0dcaa0f3" />
<img width="1333" height="693" alt="image" src="https://github.com/user-attachments/assets/bb28af5a-30e8-4c37-a477-43e8fb3690ad" />


