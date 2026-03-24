package com.bedirhan.cityeventmonitor.service;

import com.bedirhan.cityeventmonitor.model.NewsType;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class NewsTypeClassifier {

    private final Map<NewsType, List<String>> keywordMap = new EnumMap<>(NewsType.class);
    private final Map<NewsType, List<String>> positiveContextMap = new EnumMap<>(NewsType.class);
    private final Map<NewsType, List<String>> negativeContextMap = new EnumMap<>(NewsType.class);
    private final List<NewsType> priorityOrder = List.of(
            NewsType.TRAFIK_KAZASI,
            NewsType.YANGIN,
            NewsType.ELEKTRIK_KESINTISI,
            NewsType.HIRSIZLIK,
            NewsType.KULTUREL_ETKINLIK
    );

    public NewsTypeClassifier() {
        keywordMap.put(NewsType.TRAFIK_KAZASI, List.of(
                "trafik kazası", "kaza", "çarpıştı", "çarpışma", "otomobil", "araç", "kamyon", "motosiklet",
                "yaralı", "ölümlü kaza", "dorsesi", "refüj", "emniyet kemeri", "alkollü sürücü", "kaza yaptı",
                "araç takla attı", "trafik kazası", "ölümlü", "yaralılar", "ambulans", "trafik"
        ));
        keywordMap.put(NewsType.YANGIN, List.of(
                "yangın", "alev", "itfaiye", "yanarak", "yanan", "dumanlar", "yanıcı", "yangın söndürüldü",
                "kundaklama", "alev aldı", "tutuştu", "söndürüldü", "yangından", "duman"
        ));
        keywordMap.put(NewsType.ELEKTRIK_KESINTISI, List.of(
                "elektrik kesintisi", "enerji kesintisi", "planlı kesinti", "bakım çalışması",
                "trafo arızası", "hat çalışması", "yüksek gerilim hattı", "aydınlatma arızası",
                "elektrik kesildi", "tedarik", "arıza", "şebeke", "sedaş", "kesinti programı"
        ));
        keywordMap.put(NewsType.HIRSIZLIK, List.of(
                "hırsızlık", "soygun", "gasp", "hırsız", "evden hırsızlık", "iş yerinden hırsızlık",
                "suçüstü", "şüpheli şahıs", "kapıyı zorlayarak", "çalındı", "çalınan", "çalıntı",
                "polis", "gözaltına", "tutuklandı", "dolandırıcılık"
        ));
        keywordMap.put(NewsType.KULTUREL_ETKINLIK, List.of(
                "konser", "festival", "etkinlik", "tiyatro", "sergi", "panel", "söyleşi", "açılış töreni",
                "kültür merkezi", "kongre merkezi", "sanatçı", "gösteri", "açılış", "temel atıldı", "temeli atıldı",
                "tören", "açıldı", "ziyaret", "buluştu", "vatandaş", "futbol", "maç", "şampiyona", "lig",
                "spor", "röportaj", "söyleşi", "müftülük", "başkan", "belediye", "toplantı", "davet",
                "sergi", "söyleşi", "canlı yayın", "röportaj", "kongre", "seminer", "eğitim", "kurs",
                "kocaelispor", "transfer", "imza", "forvet", "stoper", "orta saha", "teknik direktör", "oyuncu"
        ));

        // Türe özel bağlam sinyalleri (yanlış pozitifleri azaltmak için)
        positiveContextMap.put(NewsType.TRAFIK_KAZASI, List.of(
                "kaza", "çarpış", "yaralı", "ambulans", "sürücü", "araç", "olay yeri", "trafik"
        ));
        negativeContextMap.put(NewsType.TRAFIK_KAZASI, List.of(
                "transfer", "oyuncu", "festival", "konser", "elektrik kesintisi", "hırsızlık"
        ));

        positiveContextMap.put(NewsType.YANGIN, List.of(
                "yangın", "alev", "itfaiye", "duman", "söndür", "müdahale"
        ));
        negativeContextMap.put(NewsType.YANGIN, List.of(
                "transfer", "futbol", "konser", "elektrik kesintisi"
        ));

        positiveContextMap.put(NewsType.ELEKTRIK_KESINTISI, List.of(
                "elektrik", "enerji", "şebeke", "trafo", "sedaş", "planlı", "bakım", "hat", "gerilim"
        ));
        negativeContextMap.put(NewsType.ELEKTRIK_KESINTISI, List.of(
                "transfer", "oyuncu", "kocaelispor", "festival", "konser", "hırsızlık"
        ));

        positiveContextMap.put(NewsType.HIRSIZLIK, List.of(
                "hırsız", "çalın", "polis", "gözalt", "tutuk", "şüpheli", "soygun", "gasp"
        ));
        negativeContextMap.put(NewsType.HIRSIZLIK, List.of(
                "transfer", "maç", "konser", "elektrik kesintisi", "trafik kazası"
        ));

        positiveContextMap.put(NewsType.KULTUREL_ETKINLIK, List.of(
                "etkinlik", "konser", "festival", "tiyatro", "sergi", "söyleşi", "maç", "transfer", "oyuncu", "imza"
        ));
        negativeContextMap.put(NewsType.KULTUREL_ETKINLIK, List.of(
                "elektrik kesintisi", "trafo", "hırsızlık", "gasp", "trafik kazası", "yangın"
        ));
    }

    /**
     * İçeriğe göre haber türünü belirler.
     * Birden fazla kategoriye uyuyorsa, önce en yüksek skoru; eşitlikte priorityOrder'a göre seçim yapar.
     */
    public NewsType classify(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return null;
        }

        String content = rawContent.toLowerCase(Locale.forLanguageTag("tr-TR"));

        int bestScore = 0;
        NewsType bestType = null;

        for (NewsType type : NewsType.values()) {
            int score = scoreForType(type, content);
            if (score > bestScore) {
                bestScore = score;
                bestType = type;
            } else if (score == bestScore && score > 0 && bestType != null) {
                // Eşit skor varsa öncelik sırasına göre karar ver
                if (priorityOrder.indexOf(type) < priorityOrder.indexOf(bestType)) {
                    bestType = type;
                }
            }
        }

        // Hiçbir türe uymayan haberler (söyleşi, spor, açılış vb.) için varsayılan: Kültürel Etkinlik
        if (bestType == null) {
            return NewsType.KULTUREL_ETKINLIK;
        }
        return bestType;
    }

    private int scoreForType(NewsType type, String content) {
        List<String> keywords = keywordMap.get(type);
        if (keywords == null || keywords.isEmpty()) {
            return 0;
        }

        int score = 0;
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            String normalizedKeyword = keyword.toLowerCase(Locale.forLanguageTag("tr-TR"));
            if (content.contains(normalizedKeyword)) {
                // Çok belirleyici çok-kelimeli ifadeleri daha yüksek puanla
                score += normalizedKeyword.contains(" ") ? 2 : 1;
            }
        }

        int positiveMatches = countContains(content, positiveContextMap.get(type));
        int negativeMatches = countContains(content, negativeContextMap.get(type));

        // Pozitif bağlam skoru hafif artırır, negatif bağlam skoru düşürür.
        score += positiveMatches;
        score -= (negativeMatches * 2);

        // Güvenlik: skor negatif olmasın.
        if (score < 0) {
            score = 0;
        }

        return score;
    }

    private int countContains(String content, List<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String term : terms) {
            if (term != null && !term.isBlank() && content.contains(term)) {
                count++;
            }
        }
        return count;
    }
}

