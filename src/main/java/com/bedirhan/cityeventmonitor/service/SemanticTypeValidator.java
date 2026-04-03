package com.bedirhan.cityeventmonitor.service;

import com.bedirhan.cityeventmonitor.model.NewsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * MiniLM tabanlı anlamsal haber türü doğrulayıcı.
 * Hugging Face'in yeni yapısına uygun olarak doğrudan SentenceSimilarityPipeline kullanır.
 * Metni, tüm referans cümlelere tek hamlede gönderip kosinüs benzerliğini Server tarafında hesaplatır.
 */
@Service
public class SemanticTypeValidator {

    private static final Logger log = LoggerFactory.getLogger(SemanticTypeValidator.class);

    private static final int MAX_VALIDATION_CHARS = 500;

    private static final Map<NewsType, List<String>> REFERENCE_TEXTS = new EnumMap<>(NewsType.class);

    static {
        REFERENCE_TEXTS.put(NewsType.TRAFIK_KAZASI, List.of(
                "Trafik kazası meydana geldi araçlar çarpıştı otomobil kamyon tırla çarpıştı",
                "Kaza nedeniyle yol trafiğe kapandı sürücü yaralandı ambulans sevk edildi",
                "Otomobil refüje çarptı motosiklet devrildi alkollü sürücü kaza yaptı",
                "Araç takla attı sürücü hayatını kaybetti trafik polisi olay yerinde"
        ));
        REFERENCE_TEXTS.put(NewsType.YANGIN, List.of(
                "Yangın çıktı itfaiye ekipleri müdahale etti alevler söndürüldü",
                "Binada çıkan yangın büyük hasara yol açtı duman yükseldi kundaklama",
                "Ev veya eşyalar alev aldı itfaiye saatlerce uğraştı panik yaşandı",
                "Şofben alev aldı ev duman altında kaldı itfaiye müdahale etti"
        ));
        REFERENCE_TEXTS.put(NewsType.ELEKTRIK_KESINTISI, List.of(
                "Planlı elektrik kesintisi yapılacak saatlerce elektrik verilmeyecek SEDAŞ duyurdu",
                "Trafo arızası nedeniyle elektrikler kesildi ekipler çalışma başlattı",
                "Elektrik kesintisi enerji verilemeyecek bakım çalışması hat arızası"
        ));
        REFERENCE_TEXTS.put(NewsType.HIRSIZLIK, List.of(
                "Hırsızlar iş yerine girerek kasayı soydu şüpheli yakalandı polis gözaltı",
                "Gasp olayında mağdur darp edildi zanlı tutuklandı çalındı",
                "Evden hırsızlık yapan şüpheli suçüstü yakalandı kapıyı zorladı"
        ));
        REFERENCE_TEXTS.put(NewsType.KULTUREL_ETKINLIK, List.of(
                "Konser ve festival etkinliği düzenlendi ünlü sanatçılar sahne aldı tören",
                "Futbol maçında takımlar karşılaştı transfer gerçekleşti imza töreni oyuncu",
                "Tiyatro gösterisi sergi açıldı panel söyleşi kültür merkezi etkinlik",
                "Farkındalık etkinliği düzenlendi seminer katılımcılara bilgi verildi",
                "Eğitim programı gerçekleştirildi öğrenciler bilgilendirildi sempozyum",
                "Deprem afet bilinçlendirme etkinliği bilgilendirme toplantısı yapıldı",
                "Farkındalık günü kutlandı konferans workshop sivil toplum etkinliği"
        ));
    }

    private final EmbeddingService embeddingService;

    @Value("${nlp.semantic-validation.enabled:true}")
    private boolean validationEnabled;

    @Value("${nlp.semantic-validation.min-similarity:0.35}")
    private double minSimilarityThreshold;

    // Düzleştirilmiş referans listesi (Hugging Face'e tek hamlede yollamak için)
    private final List<String> flatReferenceTexts = new ArrayList<>();
    private final List<NewsType> flatReferenceTypes = new ArrayList<>();

    public SemanticTypeValidator(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostConstruct
    public void initReferences() {
        if (!validationEnabled) {
            log.info("[SemanticValidator] Semantic validasyon devre dışı");
            return;
        }

        if (!embeddingService.isRemoteAvailable()) {
            log.warn("=========================================================================");
            log.warn("[SemanticValidator] HUGGING FACE API KEY BULUNAMADI VEYA OKUNAMADI!");
            log.warn("[SemanticValidator] Uygulamanız AI olmadan (eski kelime sistemiyle) çalışacak.");
            log.warn("=========================================================================");
            return;
        }

        // Tüm liste dinamik çekim için düzleştiriliyor
        for (Map.Entry<NewsType, List<String>> entry : REFERENCE_TEXTS.entrySet()) {
            for (String sentence : entry.getValue()) {
                flatReferenceTexts.add(sentence);
                flatReferenceTypes.add(entry.getKey());
            }
        }
        log.info("[SemanticValidator] Toplam {} referans cümle analize hazır.", flatReferenceTexts.size());
    }

    /**
     * Keyword türünün semantik olarak "yeterli" sayılması için gereken minimum skor.
     * Keyword türünün kendi referans cümleleriyle benzerliği bu eşiğin altındaysa
     * keyword sınıflandırması reddedilir ve DIGER'e düşürülür.
     */
    @Value("${nlp.semantic-validation.confirm-threshold:0.30}")
    private double confirmThreshold;

    /**
     * Semantik doğrulama: Keyword sınıflandırıcısının sonucunu anlamsal olarak kontrol eder.
     *
     * <p><b>TEMEL KURAL: Semantik validator ASLA bir spesifik türü başka bir spesifik türe çeviremez.</b></p>
     *
     * <p>Keyword sınıflandırıcısı kural tabanlıdır ve "hırsızlık", "yangın" gibi açık keyword
     * eşleşmeleri yapar — bu, MiniLM'nin belirsiz benzerlik skorlarından çok daha güvenilirdir.</p>
     *
     * <p>Bu validator yalnızca iki şey yapabilir:</p>
     * <ol>
     *   <li><b>ONAYLA</b> — Keyword türünün semantik skoru yeterli → keyword korunur</li>
     *   <li><b>DIGER'e düşür</b> — Keyword türünün kendi semantik skoru çok düşük →
     *       keyword eşleşmesi yanlış pozitif olabilir, DIGER'e düşür</li>
     * </ol>
     */
    public NewsType validate(String title, String contentSnippet, NewsType keywordType) {
        if (keywordType == NewsType.DIGER) {
            return NewsType.DIGER;
        }

        if (!validationEnabled || flatReferenceTexts.isEmpty()) {
            return keywordType;
        }

        try {
            String validationText = buildValidationText(title, contentSnippet);

            List<Double> scores = embeddingService.calculateSimilarities(validationText, flatReferenceTexts);

            if (scores == null || scores.isEmpty() || scores.size() != flatReferenceTexts.size()) {
                return keywordType; // API hata verdiyse keyword'e güven
            }

            // Keyword türünün kendi referans cümleleriyle en yüksek benzerlik skorunu bul
            double keywordBestScore = 0.0;
            for (int i = 0; i < scores.size(); i++) {
                if (flatReferenceTypes.get(i) == keywordType) {
                    keywordBestScore = Math.max(keywordBestScore, scores.get(i));
                }
            }

            // Keyword türünün kendi skoru yeterli mi?
            if (keywordBestScore >= confirmThreshold) {
                log.debug("[SemanticValidator] ONAYLANDI ✅ → {} (skor: {})",
                        keywordType, String.format("%.3f", keywordBestScore));
                return keywordType;
            }

            // Keyword türü semantik olarak zayıf — yanlış pozitif olabilir, DIGER'e düşür
            log.info("[SemanticValidator] Tür REDDEDİLDİ ⬇️ → Keyword: {} | Skor: {} < Eşik: {} | Metin: \"{}\"",
                    keywordType,
                    String.format("%.3f", keywordBestScore),
                    String.format("%.2f", confirmThreshold),
                    validationText);
            return NewsType.DIGER;

        } catch (Exception e) {
            log.warn("[SemanticValidator] Doğrulama hatası, keyword türü korunuyor: {}", e.getMessage());
            return keywordType;
        }
    }

    private String buildValidationText(String title, String contentSnippet) {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isBlank()) {
            sb.append(title.trim());
        }
        if (contentSnippet != null && !contentSnippet.isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(contentSnippet.trim());
        }
        String combined = sb.toString();
        return combined.length() > MAX_VALIDATION_CHARS
                ? combined.substring(0, MAX_VALIDATION_CHARS)
                : combined;
    }
}
