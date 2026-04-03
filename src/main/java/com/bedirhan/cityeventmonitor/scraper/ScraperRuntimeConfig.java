package com.bedirhan.cityeventmonitor.scraper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Kaynak başına ana sayfadan kaç haber linkinin detayının işleneceği üst sınır.
 * Eski sabit 25; artık yapılandırılabilir.
 */
@Component
public class ScraperRuntimeConfig {

    private final int maxDetailPagesPerSource;

    public ScraperRuntimeConfig(
            @Value("${scraping.max-detail-pages-per-source:-1}") int maxDetailPagesPerSource) {
        this.maxDetailPagesPerSource = maxDetailPagesPerSource > 0 ? maxDetailPagesPerSource : Integer.MAX_VALUE;
    }

    public int getMaxDetailPagesPerSource() {
        return maxDetailPagesPerSource;
    }
}
