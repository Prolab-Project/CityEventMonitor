package com.bedirhan.cityeventmonitor.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LocationExtractor {

    /** Kocaeli ilçe isimleri (küçük harf, Türkçe karakterli) */
    private static final List<String> DISTRICTS = List.of(
            "izmit",
            "gebze",
            "darıca",
            "çayırova",
            "kartepe",
            "başiskele",
            "körfez",
            "kandıra",
            "gölcük",
            "derince",
            "dilovası",
            "karamürsel"
    );

    /** ASCII / yazım varyantları → canonical ilçe adı (küçük) */
    private static final Map<String, String> DISTRICT_ALIASES = Map.ofEntries(
            Map.entry("dilovasi", "dilovası"),
            Map.entry("korfez", "körfez"),
            Map.entry("cayirova", "çayırova"),
            Map.entry("basiskele", "başiskele"),
            Map.entry("kandira", "kandıra"),
            Map.entry("golcuk", "gölcük"),
            Map.entry("karamursel", "karamürsel"),
            Map.entry("darica", "darıca")
    );

    /**
     * Spesifik konum anahtar kelimeleri — önce daha uzun/spesifik olanlar geliyor.
     * Format: [arama kelimesi (küçük), görüntülenecek ad]
     */
    private static final List<String[]> SPECIFIC_PLACE_ENTRIES = List.of(
            // Otoyollar / Ana yollar
            new String[]{"tem otoyolu", "TEM Otoyolu"},
            new String[]{"kuzey marmara otoyolu", "Kuzey Marmara Otoyolu"},
            new String[]{"anadolu otoyolu", "Anadolu Otoyolu (TEM)"},
            new String[]{"d-100 karayolu", "D-100 Karayolu"},
            new String[]{"d100 karayolu", "D-100 Karayolu"},
            new String[]{"e-5 karayolu", "E-5 Karayolu (D-100)"},
            new String[]{"sahil yolu", "Sahil Yolu"},
            // Tüneller
            new String[]{"korutepe tüneli", "Korutepe Tüneli (TEM)"},
            new String[]{"gültepe tüneli", "Gültepe Tüneli (TEM)"},
            new String[]{"gebze tüneli", "Gebze Tüneli"},
            new String[]{"çevreyolu tüneli", "Çevreyolu Tüneli"},
            // Köprüler ve kavşaklar
            new String[]{"osmangazi köprüsü", "Osmangazi Köprüsü"},
            new String[]{"osmangazi", "Osmangazi Köprüsü"},
            new String[]{"arifiye kavşağı", "Arifiye Kavşağı"},
            new String[]{"arifiye", "Arifiye Kavşağı"},
            // Mahalle ve semtler (genelden özele sıralı)
            new String[]{"yahyakaptan mahallesi", "Yahyakaptan Mahallesi"},
            new String[]{"yahyakaptan", "Yahyakaptan Mahallesi"},
            new String[]{"bekirdere mahallesi", "Bekirdere Mahallesi"},
            new String[]{"bekirdere", "Bekirdere Mahallesi"},
            new String[]{"kocaeli kongre merkezi", "Kocaeli Kongre Merkezi"},
            new String[]{"organize sanayi bölgesi", "Organize Sanayi Bölgesi"},
            new String[]{"sanayi sitesi", "Sanayi Sitesi"},
            // Alt ilçe / belde isimleri
            new String[]{"hereke", "Hereke"},
            new String[]{"değirmendere", "Değirmendere"},
            new String[]{"akarca", "Akarca"},
            new String[]{"kaletaşı", "Kaletaşı"},
            new String[]{"tavşantepe", "Tavşantepe"},
            new String[]{"kullar", "Kullar"},
            new String[]{"nusretiye", "Nusretiye"},
            new String[]{"tavşancıl", "Tavşancıl"},
            new String[]{"arslanbey", "Arslanbey (Körfez)"},
            new String[]{"yarımca", "Yarımca"},
            new String[]{"altınova", "Altınova (Gölcük)"},
            new String[]{"donanma", "Donanma Mahallesi (Gölcük)"},
            new String[]{"mehmetçik", "Mehmetçik Mahallesi"},
            new String[]{"otogar", "Otogar"},
            new String[]{"otogarı", "Otogar"},
            new String[]{"çarşı", "Çarşı"}
    );

    /**
     * TEM/D-100 vb. kısaltma/anahtar kelimelere göre eşleştirme (PLACE_KEYWORDS'te karşılık yoksa)
     * Bu pattern, PLACE_KEYWORDS'ten önce kontrol edilmez; fallback olarak çalışır.
     */
    private static final Pattern HIGHWAY_PATTERN = Pattern.compile(
            "\\b(tem|kuzey marmara|d-?100|e-?5|osmangazi köprüsü|osmangazi|korutepe|gültepe|anadolu otoyolu)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    // Genel adres kalıpları
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
            "([\\p{L}0-9\\-\\.\\s]+?(mahallesi|caddesi|sokak|bulvarı|köprüsü|karayolu))",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    // Daha esnek adres kalıpları (kısaltmalar + farklı yazımlar)
    private static final Pattern ADDRESS_PATTERN_EXTENDED = Pattern.compile(
            "([\\p{L}0-9\\-\\.\\s']+?(mah\\.?|mahallesi|sok\\.?|sokak|cad\\.?|caddesi|blv\\.?|bulvarı|meydanı|köyü|mevkii))",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    /**
     * Haber metninden ilçe ve mümkün olduğunca spesifik konum bilgisini çıkarmaya çalışır.
     * Konum bulunamazsa district ve locationText null döner.
     *
     * locationText, haritada ve popup'ta gösterilecek birleşik metni içerir:
     *   - Örn: "İzmit, TEM Otoyolu" veya "Gölcük, Altınova" veya "Gebze, Yahyakaptan Mahallesi"
     */
    public LocationResult extract(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return new LocationResult(null, null);
        }

        String contentLower = rawContent.toLowerCase(Locale.forLanguageTag("tr-TR"));

        String district = findDistrict(contentLower);
        String specificLocation = findSpecificLocation(contentLower);

        // İlçe doğrudan bulunamadıysa, bulunan konum metninin geçtiği bağlamdan ilçe tahmin etmeye çalış
        if (district == null && specificLocation != null && !specificLocation.isBlank()) {
            district = inferDistrictFromContext(contentLower, specificLocation);
        }

        // Konum bilgisi yoksa harita için zorlama fallback kullanma.
        // Bu durumda haber listede görünür, fakat haritada marker olarak gösterilmez.
        if (district == null && (specificLocation == null || specificLocation.isBlank())) {
            return new LocationResult(null, null);
        }

        // Birleşik displayText oluştur: "İlçe, Spesifik Konum" veya sadece biri
        String displayLocationText = buildDisplayLocationText(district, specificLocation);

        return new LocationResult(
                district,
                displayLocationText
        );
    }

    /**
     * İlçe adı ve spesifik konumu birleştirerek görüntülenecek metni oluşturur.
     * Örn: district="İzmit", specific="TEM Otoyolu" → "İzmit, TEM Otoyolu"
     * Eğer spesifik konum zaten ilçe adını içeriyorsa tekrar etme.
     */
    private String buildDisplayLocationText(String district, String specificLocation) {
        if (district == null || district.isBlank()) {
            return specificLocation != null ? specificLocation.trim() : null;
        }
        if (specificLocation == null || specificLocation.isBlank()) {
            return district.trim();
        }

        String districtLower = district.toLowerCase(Locale.forLanguageTag("tr-TR")).trim();
        String specificLower = specificLocation.toLowerCase(Locale.forLanguageTag("tr-TR")).trim();

        // Spesifik konum zaten ilçe adını içeriyorsa sadece spesifik konumu döndür
        if (specificLower.contains(districtLower)) {
            return specificLocation.trim();
        }

        // Spesifik konum, ilçe adının kendisi ise sadece ilçeyi döndür
        if (specificLower.equals(districtLower)) {
            return district.trim();
        }

        return district.trim() + ", " + specificLocation.trim();
    }

    private String findDistrict(String contentLower) {
        // Birden fazla ilçe geçtiğinde ilk bulduğunu almak hatalı sonuç üretebiliyor.
        // Bu yüzden ilçe adaylarını skorlayıp en güçlü eşleşmeyi seçiyoruz.
        Map<String, Integer> scores = new HashMap<>();

        for (String district : DISTRICTS) {
            int score = 0;
            if (contentLower.contains(district)) score += 1;
            if (contentLower.contains(district + " ilçesi")) score += 3;
            if (contentLower.contains(district + " ilçesinde")) score += 3;
            if (contentLower.contains(district + "'de") || contentLower.contains(district + "'da")) score += 2;
            if (contentLower.contains(district + "'nde") || contentLower.contains(district + "'nda")) score += 2;
            if (contentLower.contains(district + "'te") || contentLower.contains(district + "'ta")) score += 2;
            if (score > 0) {
                scores.put(district, Math.max(scores.getOrDefault(district, 0), score));
            }
        }

        for (Map.Entry<String, String> e : DISTRICT_ALIASES.entrySet()) {
            String alias = e.getKey();
            String canonical = e.getValue();
            int score = 0;
            if (contentLower.contains(alias)) score += 1;
            if (contentLower.contains(alias + " ilçesi")) score += 3;
            if (contentLower.contains(alias + " ilçesinde")) score += 3;
            if (contentLower.contains(alias + "'de") || contentLower.contains(alias + "'da")) score += 2;
            if (score > 0) {
                scores.put(canonical, Math.max(scores.getOrDefault(canonical, 0), score));
            }
        }

        String best = null;
        int bestScore = -1;
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            if (e.getValue() > bestScore) {
                best = e.getKey();
                bestScore = e.getValue();
            }
        }

        return best != null ? capitalizeTurkish(best) : null;
    }

    /**
     * Haber metninden en spesifik konum bilgisini bulur.
     * Önce bilinen yer/otoyol listesini tarar, sonra regex pattern'leri dener.
     */
    private String findSpecificLocation(String contentLower) {
        // 1) Bilinen spesifik yer listesi (öncelik sırası önemli — uzun önce)
        for (String[] entry : SPECIFIC_PLACE_ENTRIES) {
            String keyword = entry[0];
            String displayName = entry[1];
            if (contentLower.contains(keyword)) {
                return displayName;
            }
        }

        // 2) Genel adres pattern'i ile yakalamaya çalış
        Matcher matcher = ADDRESS_PATTERN.matcher(contentLower);
        if (matcher.find()) {
            String match = matcher.group(1);
            // Çok uzun eşleşmeleri reddet (hatalı pattern eşleşmesi)
            if (match.length() <= 80) {
                return capitalizeTurkish(match.trim());
            }
        }

        // 3) Genişletilmiş adres pattern'i
        Matcher extendedMatcher = ADDRESS_PATTERN_EXTENDED.matcher(contentLower);
        if (extendedMatcher.find()) {
            String match = extendedMatcher.group(1);
            if (match.length() <= 80) {
                return capitalizeTurkish(match.trim());
            }
        }

        // 4) Fallback: Otoyol kısaltma/anahtar kelimesi yakalanabilirse
        Matcher hwMatcher = HIGHWAY_PATTERN.matcher(contentLower);
        if (hwMatcher.find()) {
            String hw = hwMatcher.group(1).toLowerCase(Locale.forLanguageTag("tr-TR"));
            if (hw.equals("tem")) return "TEM Otoyolu";
            if (hw.startsWith("d") && hw.contains("100")) return "D-100 Karayolu";
            if (hw.startsWith("e") && hw.contains("5")) return "E-5 Karayolu (D-100)";
            if (hw.equals("kuzey marmara")) return "Kuzey Marmara Otoyolu";
            if (hw.equals("anadolu otoyolu")) return "Anadolu Otoyolu (TEM)";
            if (hw.equals("osmangazi") || hw.equals("osmangazi köprüsü")) return "Osmangazi Köprüsü";
            if (hw.equals("korutepe")) return "Korutepe Tüneli (TEM)";
            if (hw.equals("gültepe")) return "Gültepe Tüneli (TEM)";
            return capitalizeTurkish(hwMatcher.group(1));
        }

        return null;
    }

    private String inferDistrictFromContext(String contentLower, String specificLocation) {
        String loc = specificLocation.toLowerCase(Locale.forLanguageTag("tr-TR")).trim();

        // İlk olarak bilinen spesifik lokasyonların raw keyword'ünü bul
        String keyword = null;
        for (String[] entry : SPECIFIC_PLACE_ENTRIES) {
            if (entry[1].equals(specificLocation) || entry[0].equals(loc)) {
                keyword = entry[0];
                break;
            }
        }

        String searchLoc = keyword != null ? keyword : loc;
        int idx = contentLower.indexOf(searchLoc);
        if (idx < 0) {
            idx = contentLower.indexOf(loc);
        }
        if (idx < 0) {
            return null;
        }

        int start = Math.max(0, idx - 140);
        int end = Math.min(contentLower.length(), idx + searchLoc.length() + 140);
        String window = contentLower.substring(start, end);

        String district = findDistrict(window);
        if (district != null) {
            return district;
        }
        return null;
    }

    private String capitalizeTurkish(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        text = text.trim();
        // Birden fazla kelimeli metinlerde her kelimenin baş harfini büyüt
        String[] words = text.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) sb.append(" ");
            String w = words[i].toLowerCase(Locale.forLanguageTag("tr-TR"));
            if (!w.isEmpty()) {
                sb.append(w.substring(0, 1).toUpperCase(Locale.forLanguageTag("tr-TR")));
                sb.append(w.substring(1));
            }
        }
        return sb.toString();
    }
}
