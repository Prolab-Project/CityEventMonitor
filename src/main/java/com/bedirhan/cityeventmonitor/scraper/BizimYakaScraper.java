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
public class BizimYakaScraper implements NewsScraper {

    private static final Logger log = LoggerFactory.getLogger(BizimYakaScraper.class);
    private static final String BASE_URL = "https://bizimyaka.com";

    @Override
    public List<RawNews> scrape(int days) {
        List<RawNews> results = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(BASE_URL).get();
            Elements links = doc.select("a[href*=\"/haber/\"]");

            int count = 0;
            for (Element link : links) {
                RawNews raw = extractFromLink(link);
                if (raw != null) {
                    raw.setSourceName(getSourceName());
                    results.add(raw);
                }
                if (++count >= 50) {
                    break;
                }
            }
        } catch (IOException e) {
            log.error("Bizim Yaka scraping sırasında hata oluştu: {}", e.getMessage(), e);
        }

        return results;
    }

    private RawNews extractFromLink(Element link) {
        String href = link.attr("href");
        if (href == null || href.isBlank()) {
            return null;
        }
        String url = href.startsWith("http") ? href : BASE_URL + href;

        String title = link.attr("title");
        if (title == null || title.isBlank()) {
            title = link.text();
        }
        if (title == null || title.isBlank()) {
            return null;
        }

        RawNews raw = new RawNews();
        raw.setTitle(title);
        raw.setContent("");
        raw.setUrl(url);
        raw.setRawDate(null);
        raw.setRawLocationText(null);

        return raw;
    }

    @Override
    public String getSourceName() {
        return "Bizim Yaka";
    }
}

