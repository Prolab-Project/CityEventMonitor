package com.bedirhan.cityeventmonitor.scraper;

import com.bedirhan.cityeventmonitor.model.RawNews;
import org.jsoup.Jsoup;
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
    private static final String BASE_URL = "https://yenikocaeli.com";
    private static final int MAX_DETAIL_PAGES = 25;
    private static final int DELAY_MS = 400;

    @Override
    public List<RawNews> scrape(int days) {
        List<RawNews> results = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(BASE_URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();

            Elements links = doc.select("a[href*=\"/haber/\"]");
            int count = 0;
            for (Element link : links) {
                if (count >= MAX_DETAIL_PAGES) break;

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

        String url = href.startsWith("http") ? href : BASE_URL + href;
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
            Document detail = Jsoup.connect(raw.getUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();

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
