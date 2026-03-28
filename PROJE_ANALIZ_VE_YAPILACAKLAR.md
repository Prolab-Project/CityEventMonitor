# Yazlab II – Proje 1: Analiz ve Yapılacaklar

Bu doküman, PDF isterleri ile mevcut kod tabanını karşılaştırarak **mevcut durumu** ve **yapılacak işleri** listeler.

---

## Mevcut Durum (Özet)

- **Backend**: Spring Boot (Java 17), MongoDB, REST API, CORS
- **Frontend**: React + Vite, Google Maps entegrasyonu
- **Haber türleri**: 5 zorunlu tür (`NewsType`)
- **Scraping**: 5 kaynak için scraper sınıfları mevcut; detay sayfası çekimi eklendi (content/date/location doldurma)
- **Ön işleme**: HTML temizliği, normalizasyon (`TextPreprocessor`)
- **Tür sınıflandırma**: anahtar kelime tabanlı (`NewsTypeClassifier`)
- **Konum çıkarımı**: ilçe + adres pattern (`LocationExtractor`)
- **Duplicate**: URL exact + benzerlik eşiği >= 0.90 (tek kayıtta sources/urls birleştirme)
- **Geocoding**: Google Geocoding API + cache; **geocoding fail ise kayıt oluşturulmuyor**
- **Arayüz**: filtreler + liste + harita; marker tür bazlı renklendirildi; infowindow tüm kaynak/linkleri gösteriyor

---

## Eksikler / İyileştirmeler (Öncelik Sırası)

### Öncelik 1 (Kritik – şartname uyumu)

1. **API anahtarları güvenliği**
   - Backend: `GEOCODING_API_KEY` (env) veya `application-local.properties` (git dışı)
   - Frontend: `VITE_GOOGLE_MAPS_API_KEY` `.env/.env.local` (git dışı)

2. **Geocoding başarısızsa kayıt işlenmemesi**
   - Yeni kayıtlar için uygulanıyor; scraping raporlaması ve UI akışı doğrulanmalı.

### Öncelik 2 (Önemli – veri kalitesi)

3. **Gerçek “son 3 gün” kapsamı (site bazlı tarih selector iyileştirme)**
   - **Şu an**: `rawDate` parse edilebiliyorsa filtre doğru; fakat bazı sitelerde tarih selector’ı tutmazsa `rawDate` null kalıp haber “kabul” ediliyor.
   - **Yapılacak**:
     - Her site için **siteye özel tarih selector’ları** ekle/iyileştir (ör. `time[datetime]`, `.date`, `.tarih`, `meta[property=article:published_time]` varyantları).
     - `rawDate` çıkarım oranını artır; mümkünse ISO formatı normalize et.
     - Parse başarısızsa “kabul” yerine opsiyonel olarak **atılsın** seçeneği (konfig) düşün.
     - Rapor: “tarih bulunamazsa son N gün varsayımı” kararını açıkça belirt.

4. **Detay sayfası içerik temizliği**
   - Bazı sitelerde menü/etiket (“video haber”, “galeri”, “analiz” vb.) içerik metnine karışabiliyor.
   - Siteye özel içerik container’ı + gereksiz blokları çıkarma (breadcrumbs, tags, related).

5. **Konum çıkarımı doğruluğu hala değişken (kısmi iyileştirildi)**
   - Mahalle/sokak kalıpları genişletildi (`mah./sok./cad./blv.` ve varyantları) ve bağlamsal ilçe tahmini eklendi.
   - Buna rağmen bazı haberlerde ilçe/konum boş kalabilir; bu durumda haber listede kalır, haritada görünmez (kural doğru, veri kalitesi düşük).
   - Kalan iyileştirme: siteye özel konum selector'larını artırma ve yerel mahalle/sokak sözlüğünü genişletme.

6. **Kaynak adı ↔ URL eşlemesi (opsiyonel ama güçlü)**
   - Şu an `sources` ve `urls` ayrı set; bire bir eşleştirme yok.
   - Daha iyi gösterim için backend’de `sourceLinks: [{source,url}]` yapısı düşünülebilir.

### Öncelik 3 (Teslim / rapor)

7. **LaTeX (IEEE) raporu**
   - En az 4 sayfa; kullanılan anahtar kelimeler, konum çıkarımı yöntemi, duplicate/benzerlik, geocoding+cache, UI ekran görüntüleri.

---

## Hızlı Kontrol Listesi (Teslim öncesi)

- [ ] 5 siteden son 3 gün haber geliyor mu?
- [ ] Her kayıtta: tür, başlık, içerik, yayın tarihi, kaynak, link, konum metni, lat/lng var mı?
- [ ] Duplicate (>=0.90) merge düzgün mü; birden çok kaynak tek kayıtta listeleniyor mu?
- [ ] Konum yoksa haritada marker çıkmıyor mu?
- [ ] Marker tür bazlı farklı mı; infowindow başlık/tarih/kaynak/link içeriyor mu?

