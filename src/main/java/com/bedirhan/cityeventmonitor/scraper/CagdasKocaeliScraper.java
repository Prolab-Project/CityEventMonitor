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
    public List<RawNews> scrape(int days) {
        // Şimdilik basit bir yaklaşım: ana sayfadaki /haber/ linklerini çekiyoruz.
        // İleride kategoriye ve tarihe göre sayfaları da gezecek şekilde zenginleştirilebilir.
        List<RawNews> results = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(BASE_URL).get();

            // Sitedeki HTML yapısı zamanla değişebilir. Bu nedenle mümkün olduğunca
            // genel ama anlamlı bir selector kullanıyoruz: /haber/ içeren tüm linkler.
            Elements links = doc.select("a[href*=\"/haber/\"]");

            int count = 0;
            for (Element link : links) {
                RawNews raw = extractFromLink(link);
                if (raw != null) {
                    raw.setSourceName(getSourceName());
                    results.add(raw);
                }
                // Aşırı yüklenmemesi için ilk 50 haberi almak yeterli
                if (++count >= 50) {
                    break;
                }
            }
        } catch (IOException e) {
            log.error("Çağdaş Kocaeli scraping sırasında hata oluştu: {}", e.getMessage(), e);
        }

        return results;
    }

    private RawNews extractFromLink(Element link) {
        // Link ve başlık
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

        // İçerik için şimdilik sadece başlığı kullanıyoruz; ileride detay sayfasına gidilebilir.
        String content = "";

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

