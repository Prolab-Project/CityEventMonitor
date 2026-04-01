import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class TestScraper {
    public static void main(String[] args) throws Exception {
        Document doc = Jsoup.connect("https://www.cagdaskocaeli.com.tr")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(25000)
                .get();

        System.out.println("Doc Title: " + doc.title());
        Elements links = doc.select("a[href*=\"/haber/\"]");
        System.out.println("Found links: " + links.size());

        for(Element link : links) {
            String href = link.attr("href");
            String title = link.attr("title");
            if (title == null || title.isBlank()) title = link.text();
            
            System.out.println("Href: " + href + " | Title: " + title);
        }
    }
}
