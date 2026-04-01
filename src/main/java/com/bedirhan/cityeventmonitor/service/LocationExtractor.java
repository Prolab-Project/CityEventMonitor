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

    // Bazı bilinen yer/mahalle/cadde örnekleri
    private static final List<String> PLACE_KEYWORDS = List.of(
            "yahyakaptan mahallesi",
            "yahyakaptan",
            "bekirdere mahallesi",
            "bekirdere",
            "d-100 karayolu",
            "d100 karayolu",
            "otogar",
            "otogarı",
            "çarşı",
            "kocaeli kongre merkezi",
            "sahil yolu",
            "sanayi sitesi",
            "organize sanayi bölgesi",
            "akarca",
            "hereke",
            "değirmendere",
            "gölcük",
            "körfez",
            "izmit"
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
     */
    public LocationResult extract(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return new LocationResult(null, null);
        }

        String contentLower = rawContent.toLowerCase(Locale.forLanguageTag("tr-TR"));

        String district = findDistrict(contentLower);
        String locationText = findLocationText(contentLower);

        // İlçe doğrudan bulunamadıysa, bulunan konum metninin geçtiği bağlamdan ilçe tahmin etmeye çalış
        if (district == null && locationText != null && !locationText.isBlank()) {
            district = inferDistrictFromContext(contentLower, locationText);
        }

        // İlçe var ama spesifik yer yoksa geocoding için "İlçe, Kocaeli" kullan (haritada gösterebilmek için)
        if (district != null && (locationText == null || locationText.isBlank())) {
            locationText = district + ", Kocaeli";
        }

        // Konum bilgisi yoksa harita için zorlama fallback kullanma.
        // Bu durumda haber listede görünür, fakat haritada marker olarak gösterilmez.
        if (district == null && (locationText == null || locationText.isBlank())) {
            return new LocationResult(null, null);
        }

        return new LocationResult(
                district,
                locationText != null ? locationText.trim() : null
        );
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

    private String findLocationText(String contentLower) {
        // 1) Bilinen yer/mahalle anahtar kelimeleri
        for (String place : PLACE_KEYWORDS) {
            if (contentLower.contains(place)) {
                return place;
            }
        }

        // 2) Genel adres pattern'i ile yakalamaya çalış
        Matcher matcher = ADDRESS_PATTERN.matcher(contentLower);
        if (matcher.find()) {
            String match = matcher.group(1);
            // Çok uzun eşleşmeleri reddet (hatalı pattern eşleşmesi)
            if (match.length() <= 80) {
                return match;
            }
        }

        // 3) Genişletilmiş adres pattern'i
        Matcher extendedMatcher = ADDRESS_PATTERN_EXTENDED.matcher(contentLower);
        if (extendedMatcher.find()) {
            String match = extendedMatcher.group(1);
            // Çok uzun eşleşmeleri reddet (hatalı pattern eşleşmesi)
            if (match.length() <= 80) {
                return match;
            }
        }

        return null;
    }

    private String inferDistrictFromContext(String contentLower, String locationText) {
        String loc = locationText.toLowerCase(Locale.forLanguageTag("tr-TR")).trim();
        int idx = contentLower.indexOf(loc);
        if (idx < 0) {
            return null;
        }

        int start = Math.max(0, idx - 140);
        int end = Math.min(contentLower.length(), idx + loc.length() + 140);
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
        text = text.toLowerCase(Locale.forLanguageTag("tr-TR"));
        return text.substring(0, 1).toUpperCase(Locale.forLanguageTag("tr-TR")) + text.substring(1);
    }
}

