package com.bedirhan.cityeventmonitor.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
public class TextPreprocessor {

    /**
     * Haber metnini temizler ve normalize eder.
     * - HTML taglerini temizler
     * - Fazla boşlukları ve gereksiz karakterleri sadeleştirir
     * - Lowercase normalizasyonu yapar
     * - Yaygın reklam / alakasız pattern'leri kaldırmaya çalışır
     */
    public String preprocess(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        String text = raw;

        // 1) HTML taglerini kaldır
        text = Jsoup.clean(text, Safelist.none());

        // 2) Lowercase normalizasyonu (regex'lerin eşleşmesi için önce küçük harfe çevir)
        text = text.toLowerCase();

        // 3) Yaygın reklam / alakasız pattern'leri temizle
        text = removeNoisePatterns(text);

        // 4) Gereksiz özel karakterleri sadeleştir
        // Harf, rakam, noktalama ve boşluk dışındaki karakterleri kaldır
        text = text.replaceAll("[^\\p{L}\\p{N}\\p{P}\\s]+", " ");

        // 5) Fazla boşlukları tek boşluğa indir
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }

    private String removeNoisePatterns(String text) {
        String cleaned = text;

        // "Devamı için tıklayın" gibi tipik kalıplar
        cleaned = cleaned.replaceAll("devamı için tıklayınız?.*", "");
        cleaned = cleaned.replaceAll("haberin detayları için tıklayınız?.*", "");
        cleaned = cleaned.replaceAll("kayna[kğ]a git.*", "");

        return cleaned;
    }
}

