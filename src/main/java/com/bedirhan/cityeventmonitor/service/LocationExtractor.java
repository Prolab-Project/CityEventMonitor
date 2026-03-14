package com.bedirhan.cityeventmonitor.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LocationExtractor {

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

    // Bazı bilinen yer/mahalle/cadde örnekleri
    private static final List<String> PLACE_KEYWORDS = List.of(
            "yahyakaptan mahallesi",
            "bekirdere mahallesi",
            "d-100 karayolu",
            "d100 karayolu",
            "otogar",
            "otogarı",
            "çarşı",
            "kocaeli kongre merkezi",
            "sahil yolu",
            "sanayi sitesi",
            "organize sanayi bölgesi"
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

        // Hiçbir konum bilgisi bulunamadıysa tamamen null dön
        if (district == null && (locationText == null || locationText.isBlank())) {
            return new LocationResult(null, null);
        }

        return new LocationResult(
                district,
                locationText != null ? locationText.trim() : null
        );
    }

    private String findDistrict(String contentLower) {
        for (String district : DISTRICTS) {
            // Tam kelime eşleşmesine daha yakın olması için etrafında boşluk / noktalama da kontrol edilebilir
            if (contentLower.contains(district)) {
                // Yazım formatını düzgün döndür (ilk harfi büyük, diğerleri küçük)
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

