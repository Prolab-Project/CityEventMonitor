package com.bedirhan.cityeventmonitor.service;

import com.bedirhan.cityeventmonitor.model.NewsType;
import org.springframework.stereotype.Service;

import java.util.*;

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
            NewsType.KULTUREL_ETKINLIK,
            NewsType.DIGER
    );

    /**
     * Sınıflandırma için minimum ham skor eşiği.
     * Bu eşiğin altındaki skorlar güvenilir kabul edilmez ve null döner.
     */
    private static final int MIN_RAW_SCORE = 2;

    public NewsTypeClassifier() {
        // ── Anahtar Kelimeler ──────────────────────────────────────────────
        // "trafik", "araç", "otomobil", "kamyon", "motosiklet", "ambulans", "yaralı" gibi genel trafiğe
        // veya sağlığa ait kelimeler çıkarıldı. Bunlar sadece kaza bildiren asıl kelimelere destek
        // (pozitif bağlam) olarak çalışmalıdır. Yoksa "Trafik ekipleri park denetimi yaptı" haberini
        // kaza olarak işaretler.
        keywordMap.put(NewsType.TRAFIK_KAZASI, List.of(
                "trafik kazası", "kaza", "çarpıştı", "çarpışma", "ölümlü kaza", "dorsesi",
                "refüj", "emniyet kemeri", "alkollü sürücü", "kaza yaptı", "araç takla attı", "ölümlü"
        ));
        keywordMap.put(NewsType.YANGIN, List.of(
                "yangın", "alev", "itfaiye", "yanarak", "yanan", "dumanlar", "yanıcı", "yangın söndürüldü",
                "kundaklama", "alev aldı", "tutuştu", "söndürüldü", "yangından"
        ));
        keywordMap.put(NewsType.ELEKTRIK_KESINTISI, List.of(
                "elektrik kesintisi", "enerji kesintisi", "planlı kesinti", "bakım çalışması",
                "trafo arızası", "hat çalışması", "yüksek gerilim hattı", "aydınlatma arızası",
                "elektrik kesildi", "tedarik", "arıza", "şebeke", "sedaş", "kesinti programı"
        ));
        // "polis", "gözaltına", "tutuklandı" gibi genel asayiş kelimeleri hırsızlık keyword'ünden
        // çıkarıldı. Başka asayiş olayları (ör: operasyon) yanlışlıkla hırsızlık sanılmasın.
        keywordMap.put(NewsType.HIRSIZLIK, List.of(
                "hırsızlık", "soygun", "gasp", "hırsız", "evden hırsızlık", "iş yerinden hırsızlık",
                "suçüstü", "şüpheli şahıs", "kapıyı zorlayarak", "çalındı", "çalınan", "çalıntı", "dolandırıcılık"
        ));
        // Temizlenmiş liste: "başkan", "belediye", "vatandaş", "toplantı", "ziyaret",
        // "buluştu", "açıldı", "açılış", "davet", "müftülük" gibi çok genel kelimeler çıkarıldı.
        // Tekrar eden kelimeler ("söyleşi" 3x, "sergi" 2x, "röportaj" 2x) tekile indirildi.
        keywordMap.put(NewsType.KULTUREL_ETKINLIK, List.of(
                "konser", "festival", "etkinlik", "tiyatro", "sergi", "panel", "söyleşi", "açılış töreni",
                "kültür merkezi", "kongre merkezi", "sanatçı", "gösteri", "tören",
                "futbol", "maç", "şampiyona", "lig", "spor", "röportaj",
                "canlı yayın", "kongre", "seminer", "eğitim", "kurs",
                "kocaelispor", "transfer", "imza", "forvet", "stoper", "orta saha", "teknik direktör", "oyuncu"
        ));

        // ── Pozitif Bağlam Sinyalleri ──────────────────────────────────────
        positiveContextMap.put(NewsType.TRAFIK_KAZASI, List.of(
                "kaza", "çarpış", "yaralı", "ambulans", "sürücü", "araç", "olay yeri", "trafik"
        ));
        positiveContextMap.put(NewsType.YANGIN, List.of(
                "yangın", "alev", "itfaiye", "duman", "söndür", "müdahale"
        ));
        // "hat" çıkarıldı: sadece 3 karakter, "hatip" (İmam Hatip) gibi kelimelerde yanlış eşleşir.
        // "hat çalışması" zaten keyword olarak var.
        positiveContextMap.put(NewsType.ELEKTRIK_KESINTISI, List.of(
                "elektrik", "enerji", "şebeke", "trafo", "sedaş", "planlı", "gerilim"
        ));
        positiveContextMap.put(NewsType.HIRSIZLIK, List.of(
                "hırsız", "çalın", "polis", "gözalt", "tutuk", "şüpheli", "soygun", "gasp"
        ));
        positiveContextMap.put(NewsType.KULTUREL_ETKINLIK, List.of(
                "etkinlik", "konser", "festival", "tiyatro", "sergi", "söyleşi", "maç", "transfer", "oyuncu", "imza"
        ));

        // ── Negatif Bağlam Sinyalleri ──────────────────────────────────────
        negativeContextMap.put(NewsType.TRAFIK_KAZASI, List.of(
                "transfer", "oyuncu", "festival", "konser", "elektrik kesintisi", "hırsızlık"
        ));
        negativeContextMap.put(NewsType.YANGIN, List.of(
                "transfer", "futbol", "konser", "elektrik kesintisi"
        ));
        negativeContextMap.put(NewsType.ELEKTRIK_KESINTISI, List.of(
                "transfer", "oyuncu", "kocaelispor", "festival", "konser", "hırsızlık"
        ));
        negativeContextMap.put(NewsType.HIRSIZLIK, List.of(
                "transfer", "maç", "konser", "elektrik kesintisi", "trafik kazası"
        ));
        negativeContextMap.put(NewsType.KULTUREL_ETKINLIK, List.of(
                "elektrik kesintisi", "trafo", "hırsızlık", "gasp", "trafik kazası", "yangın"
        ));
    }

    /**
     * İçeriğe göre haber türünü belirler.
     *
     * <p>Algoritmik akış:</p>
     * <ol>
     *   <li>Metin tokenize edilir (kelimelerine ayrılır).</li>
     *   <li>Her tür için skor hesaplanır (anahtar kelime + bağlam).</li>
     *   <li>Ham skoru {@code MIN_RAW_SCORE} eşiğinin altında kalan türler elenir.</li>
     *   <li>Kalan türler arasında <b>normalize skor</b> (ham skor / keyword sayısı) en yüksek olan seçilir.</li>
     *   <li>Eşitlik varsa {@code priorityOrder}'a göre karar verilir.</li>
     *   <li>Hiçbir tür eşiği geçemezse {@code null} döner.</li>
     * </ol>
     */
    public NewsType classify(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return NewsType.DIGER;
        }

        String content = rawContent.toLowerCase(Locale.forLanguageTag("tr-TR"));
        List<String> tokens = tokenize(content);

        double bestNormalizedScore = 0;
        NewsType bestType = null;

        for (NewsType type : NewsType.values()) {
            // DIGER tipini sınıflandırmada atla (fallback olarak kullan)
            if (type == NewsType.DIGER) {
                continue;
            }
            ScoreResult result = scoreForType(type, content, tokens);
            if (result.rawScore < MIN_RAW_SCORE) {
                continue;
            }
            if (result.normalizedScore > bestNormalizedScore) {
                bestNormalizedScore = result.normalizedScore;
                bestType = type;
            } else if (Double.compare(result.normalizedScore, bestNormalizedScore) == 0
                       && result.rawScore > 0 && bestType != null) {
                // Eşit normalize skor → öncelik sırasına göre karar ver
                if (priorityOrder.indexOf(type) < priorityOrder.indexOf(bestType)) {
                    bestType = type;
                }
            }
        }

        return bestType != null ? bestType : NewsType.DIGER;
    }

    // ── Skor Hesaplama ──────────────────────────────────────────────────────

    record ScoreResult(int rawScore, double normalizedScore) {}

    private ScoreResult scoreForType(NewsType type, String content, List<String> tokens) {
        List<String> keywords = keywordMap.get(type);
        if (keywords == null || keywords.isEmpty()) {
            return new ScoreResult(0, 0.0);
        }

        int keywordScore = 0;
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) continue;
            String normalized = keyword.toLowerCase(Locale.forLanguageTag("tr-TR"));

            if (normalized.contains(" ")) {
                // Çok kelimeli ifade → token dizisi içinde ardışık (phrase) olarak aranır
                if (matchesPhrase(tokens, normalized)) {
                    keywordScore += 2;
                }
            } else {
                // Tek kelime → token bazlı prefix eşleşme (Türkçe ek uyumlu)
                if (matchesAnyToken(tokens, normalized)) {
                    keywordScore += 1;
                }
            }
        }

        // Bağlam sinyalleri sadece en az 1 anahtar kelime eşleştiğinde devreye girer.
        // Böylece "bakım" veya "hat" gibi genel kelimeler tek başına sınıflandırma oluşturamaz.
        int score = keywordScore;
        if (keywordScore > 0) {
            int positiveMatches = countContextMatches(tokens, content, positiveContextMap.get(type));
            int negativeMatches = countContextMatches(tokens, content, negativeContextMap.get(type));
            score += positiveMatches;
            score -= (negativeMatches * 2);
        }
        if (score < 0) score = 0;

        double normalizedScore = (double) score / keywords.size();
        return new ScoreResult(score, normalizedScore);
    }

    // ── Tokenization & Prefix Matching ──────────────────────────────────────

    /**
     * Metni kelime tokenlarına ayırır. Harf ve rakam dışı karakterler ayraç olarak kullanılır.
     */
    private List<String> tokenize(String content) {
        String[] parts = content.split("[^\\p{L}\\p{N}]+");
        List<String> tokens = new ArrayList<>();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                tokens.add(p);
            }
        }
        return tokens;
    }

    /**
     * Çok kelimeli ifadeleri (örn: "trafik kazası") token listesinde ardışık olarak arar.
     * Cümlenin son kelimesi için ön-ek (prefix) ve yumuşama kabul eder, aradaki kelimeler tam eşleşmelidir.
     */
    private boolean matchesPhrase(List<String> tokens, String phrase) {
        String[] phraseTokens = phrase.split("[^\\p{L}\\p{N}]+");
        if (phraseTokens.length == 0 || tokens.size() < phraseTokens.length) return false;

        for (int i = 0; i <= tokens.size() - phraseTokens.length; i++) {
            boolean match = true;
            for (int j = 0; j < phraseTokens.length; j++) {
                String tokenStr = tokens.get(i + j);
                String phraseToken = phraseTokens[j];

                if (j == phraseTokens.length - 1) {
                    // Son kelime: Türkçe ek veya yumuşama alabilir
                    if (!tokenStr.startsWith(phraseToken)) {
                        String softened = softenLastConsonant(phraseToken);
                        if (softened == null || !tokenStr.startsWith(softened)) {
                            match = false;
                            break;
                        }
                    }
                } else {
                    // Ortadaki kelimeler tam aynı olmalı (örn: "trafik" tam uymalı)
                    if (!tokenStr.equals(phraseToken)) {
                        match = false;
                        break;
                    }
                }
            }
            if (match) return true;
        }
        return false;
    }

    /**
     * Herhangi bir tokenın verilen anahtar kelime ile başlayıp başlamadığını kontrol eder.
     * Türkçe ünsüz yumuşamasını da (k→ğ, t→d, ç→c, p→b) dener.
     *
     * <p>Örnekler:</p>
     * <ul>
     *   <li>"yangından".startsWith("yangın") → true</li>
     *   <li>"hırsızlığı".startsWith("hırsızlığ") → true (ünsüz yumuşaması: k→ğ)</li>
     *   <li>"yakazan".startsWith("kaza") → false (substring problemi yok)</li>
     * </ul>
     */
    private boolean matchesAnyToken(List<String> tokens, String keyword) {
        for (String token : tokens) {
            if (token.startsWith(keyword)) {
                return true;
            }
        }
        // Türkçe ünsüz yumuşaması: keyword sonundaki sert ünsüzü yumuşat ve tekrar dene
        String softened = softenLastConsonant(keyword);
        if (softened != null) {
            for (String token : tokens) {
                if (token.startsWith(softened)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Türkçe ünsüz yumuşaması: k→ğ, t→d, ç→c, p→b
     * Kelime sonundaki sert ünsüz, ünlü ile başlayan ek aldığında yumuşar.
     * Örn: "hırsızlık" → "hırsızlığ-ı", "araç" → "arac-ın"
     */
    private String softenLastConsonant(String word) {
        if (word == null || word.isEmpty()) return null;
        char last = word.charAt(word.length() - 1);
        char softened;
        switch (last) {
            case 'k': softened = 'ğ'; break;
            case 't': softened = 'd'; break;
            case 'ç': softened = 'c'; break;
            case 'p': softened = 'b'; break;
            default: return null;
        }
        return word.substring(0, word.length() - 1) + softened;
    }

    private int countContextMatches(List<String> tokens, String content, List<String> terms) {
        if (terms == null || terms.isEmpty()) return 0;
        int count = 0;
        for (String term : terms) {
            if (term == null || term.isBlank()) continue;
            String normalized = term.toLowerCase(Locale.forLanguageTag("tr-TR"));
            if (normalized.contains(" ")) {
                if (matchesPhrase(tokens, normalized)) count++;
            } else {
                if (matchesAnyToken(tokens, normalized)) count++;
            }
        }
        return count;
    }
}
