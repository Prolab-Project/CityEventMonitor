package com.bedirhan.cityeventmonitor.scraper;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Haber detay sayfasından içerik, tarih ve konum metnini çıkarmak için ortak selector denemeleri.
 * Siteye özel selector'lar scraper'da önce denenebilir; bu sınıf fallback sağlar.
 */
public final class DetailPageHelper {

    private static final String[] CONTENT_SELECTORS = {
            "article .content", "article .post-content", "article .entry-content",
            ".haber-icerik", ".haber-detay", ".news-content", ".detail-text", ".post-content",
            "[itemprop=articleBody]", ".content .text", "article", ".entry-content", ".article-body"
    };

    private static final String[] DATE_SELECTORS = {
            "time[datetime]", "[itemprop=datePublished]", ".date", ".tarih", ".yayin-tarihi",
            ".news-date", ".post-date", "meta[property=article:published_time]"
    };

    private DetailPageHelper() {
    }

    /**
     * Dokümandan haberin gerçek başlığını (title) çıkarır. Anasayfadan alınan hatalı
     * (örn: Kategori adı taşıyan) başlıkları detay sayfasında ezmek için kullanılır.
     */
    public static String extractTitle(Document doc) {
        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null && !ogTitle.attr("content").isBlank()) {
            return ogTitle.attr("content").trim();
        }
        Element h1 = doc.selectFirst("h1");
        if (h1 != null && !h1.text().isBlank()) {
            return h1.text().trim();
        }
        String title = doc.title();
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        return null;
    }

    /**
     * Dokümandan haber gövdesi metnini dener; ilk eşleşen selector'ın text'ini döner.
     */
    public static String extractContent(Document doc) {
        for (String selector : CONTENT_SELECTORS) {
            try {
                Elements els = doc.select(selector);
                if (els.isEmpty()) continue;
                Element first = els.first();
                if (first != null) {
                    String text = first.text();
                    if (text != null && text.length() > 50) {
                        return text.trim();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Dokümandan yayın tarihi string'ini dener. time[datetime] için attr("datetime"), diğerleri için text.
     */
    public static String extractDate(Document doc) {
        // Önce time[datetime] veya meta content
        Elements time = doc.select("time[datetime]");
        if (!time.isEmpty()) {
            String dt = time.first().attr("datetime");
            if (dt != null && !dt.isBlank()) return dt.trim();
        }
        Elements meta = doc.select("meta[property=article:published_time], meta[name=date]");
        if (!meta.isEmpty()) {
            String c = meta.first().attr("content");
            if (c != null && !c.isBlank()) return c.trim();
        }
        for (String selector : DATE_SELECTORS) {
            if (selector.startsWith("time") || selector.startsWith("meta")) continue;
            try {
                Elements els = doc.select(selector);
                if (!els.isEmpty()) {
                    String text = els.first().text();
                    if (text != null && !text.isBlank()) return text.trim();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Konum metni (varsa) - genelde içerikte veya .location benzeri sınıfta.
     */
    public static String extractLocation(Document doc) {
        String[] locSelectors = { ".location", ".konum", ".yer", "[itemprop=address]" };
        for (String selector : locSelectors) {
            Elements els = doc.select(selector);
            if (!els.isEmpty()) {
                String text = els.first().text();
                if (text != null && !text.isBlank()) return text.trim();
            }
        }
        return null;
    }
}
