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

        // İlçe var ama spesifik yer yoksa geocoding için "İlçe, Kocaeli" kullan (haritada gösterebilmek için)
        if (district != null && (locationText == null || locationText.isBlank())) {
            locationText = district + ", Kocaeli";
        }

        if (district == null && (locationText == null || locationText.isBlank())) {
            return new LocationResult(null, null);
        }

        return new LocationResult(
                district,
                locationText != null ? locationText.trim() : null
        );
    }

    private String findDistrict(String contentLower) {
        // 1) Standart ilçe adları
        for (String district : DISTRICTS) {
            if (contentLower.contains(district)) {
                return capitalizeTurkish(district);
            }
        }
        // 2) ASCII / yazım varyantları (örn. "dilovasi", "korfez")
        for (Map.Entry<String, String> e : DISTRICT_ALIASES.entrySet()) {
            if (contentLower.contains(e.getKey())) {
                return capitalizeTurkish(e.getValue());
            }
        }
        // 3) "X ilçesinde", "X'de" kalıplarında ilçe adı başta geçebilir
        for (String district : DISTRICTS) {
            if (contentLower.contains(district + " ilçesi") || contentLower.contains(district + " ilçesinde")
                    || contentLower.contains(district + "'de") || contentLower.contains(district + "'da")) {
                return capitalizeTurkish(district);
            }
        }
        return null;
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
            return matcher.group(1);
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

