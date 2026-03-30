package com.bedirhan.cityeventmonitor.scraper;

import com.bedirhan.cityeventmonitor.model.RawNews;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class YeniKocaeliScraper implements NewsScraper {

    private static final Logger log = LoggerFactory.getLogger(YeniKocaeliScraper.class);
    private static final String BASE_URL = "https://www.yenikocaeli.com";
    private static final int DELAY_MS = 400;

    private final ScraperRuntimeConfig scraperLimits;

    public YeniKocaeliScraper(ScraperRuntimeConfig scraperLimits) {
        this.scraperLimits = scraperLimits;
    }

    @Override
    public List<RawNews> scrape(int days) {
        List<RawNews> results = new ArrayList<>();

        try {
            Document doc = ScraperHttp.connect(BASE_URL).get();

            // Ana sayfada haber linkleri bazen `href="/haber/..."`,
            // bazen `href="haber/..."` (başında `/` yok) şeklinde gelebiliyor.
            // Fragment/tag linkleri ise `#...` içerdiği için ayrı filtreyle atıyoruz.
            Elements links = doc.select("a[href*=\"haber/\"]");
            int maxLinks = scraperLimits.getMaxDetailPagesPerSource();
            int count = 0;
            for (Element link : links) {
                if (count >= maxLinks) break;

                RawNews raw = buildFromListLink(link);
                if (raw == null) continue;

                fetchDetailPage(raw);
                raw.setSourceName(getSourceName());
                results.add(raw);
                count++;

                try {
                    Thread.sleep(DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (IOException e) {
            log.error("Yeni Kocaeli scraping sırasında hata oluştu: {}", e.getMessage(), e);
        }

        return results;
    }

    private RawNews buildFromListLink(Element link) {
        String href = link.attr("href");
        if (href == null || href.isBlank()) return null;

        // Ana sayfada "haber" kategorisi/etiketleri de linklenebiliyor.
        // Bunlar genelde `.../haber/#...` fragment içerdiği için gerçek haber detayına gitmez.
        if (href.contains("#")) return null;
        if (!href.contains("haber/")) return null;
        if (!href.endsWith(".html")) return null;

        String url;
        if (href.startsWith("http")) {
            url = href;
        } else {
            // Göreli linkte başta `/` olmayabiliyor: `haber/...` -> `https://.../haber/...`
            String normalizedHref = href.startsWith("/") ? href : "/" + href;
            url = BASE_URL + normalizedHref;
        }
        String title = link.attr("title");
        if (title == null || title.isBlank()) title = link.text();
        if (title == null || title.isBlank()) return null;

        RawNews raw = new RawNews();
        raw.setTitle(title);
        raw.setContent("");
        raw.setUrl(url);
        raw.setRawDate(null);
        raw.setRawLocationText(null);
        return raw;
    }

    private void fetchDetailPage(RawNews raw) {
        try {
            Document detail = ScraperHttp.connect(raw.getUrl()).get();

            String title = DetailPageHelper.extractTitle(detail);
            if (title != null && !title.isBlank()) {
                raw.setTitle(title);
            }

            String content = DetailPageHelper.extractContent(detail);
            if (content != null && !content.isBlank()) raw.setContent(content);

            String dateStr = DetailPageHelper.extractDate(detail);
            if (dateStr != null && !dateStr.isBlank()) raw.setRawDate(dateStr);

            String location = DetailPageHelper.extractLocation(detail);
            if (location != null && !location.isBlank()) raw.setRawLocationText(location);
        } catch (IOException e) {
            log.debug("Detay sayfası okunamadı: {} — {}", raw.getUrl(), e.getMessage());
        }
    }

    @Override
    public String getSourceName() {
        return "Yeni Kocaeli";
    }
}
