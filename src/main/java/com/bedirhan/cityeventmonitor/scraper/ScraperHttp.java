package com.bedirhan.cityeventmonitor.scraper;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

/**
 * Tüm scraper'lar için ortak HTTP ayarları: timeout, user-agent, redirect.
 * Timeout olmaması tek bir yanıtın takılması durumunda tüm /scrape isteğinin sonsuza kadar beklemesine yol açabilir.
 */
public final class ScraperHttp {

    /**
     * Güncel Chrome benzeri UA; bazı siteler basit/boş UA'yı daha agresif engelliyor.
     */
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/120.0.0.0 Safari/537.36";

    private static final int TIMEOUT_MS = 25_000;

    private ScraperHttp() {
    }

    public static Connection connect(String url) {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .maxBodySize(0)
                .followRedirects(true);
    }
}
