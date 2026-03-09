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
public class CagdasKocaeliScraper implements NewsScraper {

    private static final Logger log = LoggerFactory.getLogger(CagdasKocaeliScraper.class);
    private static final String BASE_URL = "https://www.cagdaskocaeli.com.tr";

    @Override
    public List<RawNews> scrapeLastDays(int days) {
        // Şimdilik basit bir yaklaşım: ana sayfadaki son haberleri çekiyoruz.
        // İleride kategoriye/tarihe göre sayfaları da gezecek şekilde zenginleştirilebilir.
        List<RawNews> results = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(BASE_URL).get();

            // Sitedeki HTML yapısı zamanla değişebilir. Bu nedenle CSS selector'lar
            // gerektiğinde güncellenmelidir.
            Elements articleElements = doc.select("article, .news-list .news-item, .article-item");

            for (Element el : articleElements) {
                RawNews raw = extractFromElement(el);
                if (raw != null) {
                    raw.setSourceName(getSourceName());
                    results.add(raw);
                }
            }
        } catch (IOException e) {
            log.error("Çağdaş Kocaeli scraping sırasında hata oluştu: {}", e.getMessage(), e);
        }

        return results;
    }

    private RawNews extractFromElement(Element el) {
        // Başlık
        Element titleEl = el.selectFirst("a[title], h2 a, .title a");
        if (titleEl == null) {
            return null;
        }

        String title = titleEl.attr("title");
        if (title == null || title.isBlank()) {
            title = titleEl.text();
        }

        // Link
        String href = titleEl.attr("href");
        if (href == null || href.isBlank()) {
            return null;
        }
        String url = href.startsWith("http") ? href : BASE_URL + href;

        // Özet / içerik (şimdilik kısa özet, detay sayfasına gitmiyoruz)
        Element summaryEl = el.selectFirst("p, .summary, .spot");
        String content = summaryEl != null ? summaryEl.text() : "";

        RawNews raw = new RawNews();
        raw.setTitle(title);
        raw.setContent(content);
        raw.setUrl(url);
        // Tarih ve konum metni şimdilik boş; sonraki adımlarda detay sayfasından da çekilebilir.
        raw.setRawDate(null);
        raw.setRawLocationText(null);

        return raw;
    }

    @Override
    public String getSourceName() {
        return "Çağdaş Kocaeli";
    }
}

