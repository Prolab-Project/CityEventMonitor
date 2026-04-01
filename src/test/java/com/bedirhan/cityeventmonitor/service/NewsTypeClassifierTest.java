package com.bedirhan.cityeventmonitor.service;

import com.bedirhan.cityeventmonitor.model.NewsType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NewsTypeClassifierTest {

    private final NewsTypeClassifier classifier = new NewsTypeClassifier();

    // ── Temel Sınıflandırma Testleri ────────────────────────────────────

    @Test
    void shouldClassifyTrafficAccident() {
        String text = "Kocaeli'de meydana gelen trafik kazası sonucunda iki araç çarpıştı, 3 kişi yaralandı.";

        NewsType type = classifier.classify(text);

        assertThat(type).isEqualTo(NewsType.TRAFIK_KAZASI);
    }

    @Test
    void shouldClassifyFire() {
        String text = "Gebze'de bir apartmanda çıkan yangın itfaiye ekipleri tarafından söndürüldü.";

        NewsType type = classifier.classify(text);

        assertThat(type).isEqualTo(NewsType.YANGIN);
    }

    @Test
    void shouldClassifyElectricityOutage() {
        String text = "İzmit'te planlı elektrik kesintisi nedeniyle bazı mahallelere enerji verilemeyecek.";

        NewsType type = classifier.classify(text);

        assertThat(type).isEqualTo(NewsType.ELEKTRIK_KESINTISI);
    }

    @Test
    void shouldClassifyTheft() {
        String text = "Bir iş yerinden hırsızlık yapan şüpheli şahıs polis ekiplerince suçüstü yakalandı.";

        NewsType type = classifier.classify(text);

        assertThat(type).isEqualTo(NewsType.HIRSIZLIK);
    }

    @Test
    void shouldClassifyCulturalEvent() {
        String text = "Kocaeli Kongre Merkezi'nde ünlü sanatçıların katılacağı büyük bir konser düzenlenecek.";

        NewsType type = classifier.classify(text);

        assertThat(type).isEqualTo(NewsType.KULTUREL_ETKINLIK);
    }

    @Test
    void shouldReturnNullWhenNoMatch() {
        String text = "Bugün hava sıcaklıkları mevsim normallerinin üzerinde seyredecek.";

        NewsType type = classifier.classify(text);

        assertThat(type).isNull();
    }

    // ── Null / Boş Girdi Testleri ───────────────────────────────────────

    @Test
    void shouldReturnNullForNullInput() {
        assertThat(classifier.classify(null)).isNull();
    }

    @Test
    void shouldReturnNullForBlankInput() {
        assertThat(classifier.classify("   ")).isNull();
    }

    // ── Türkçe Ünsüz Yumuşaması Testleri ───────────────────────────────

    @Test
    void shouldMatchTurkishConsonantSoftening_hirsizlik() {
        // "hırsızlık" → "hırsızlığı" (k→ğ)
        String text = "Mahallede hırsızlığı yapan kişi gözaltına alındı, şüpheli yakalandı.";

        NewsType type = classifier.classify(text);

        assertThat(type).isEqualTo(NewsType.HIRSIZLIK);
    }

    @Test
    void shouldMatchTurkishSuffix_yangin() {
        // "yangın" + Türkçe ekler: "yangından", "yangınlar"
        String text = "Gece saatlerinde çıkan yangından dolayı itfaiye müdahale etti.";

        NewsType type = classifier.classify(text);

        assertThat(type).isEqualTo(NewsType.YANGIN);
    }

    // ── Substring Yanlış Pozitif Koruması ───────────────────────────────

    @Test
    void shouldNotFalsePositiveOnSubstring() {
        // "kaza" kelimesi "yakazan" içinde substring olarak geçer ama token prefix olarak geçmez
        String text = "Turnuvayı yakazan sporcu çok mutluydu, taraftarlar sevinçle alkışladı.";

        NewsType type = classifier.classify(text);

        // "kaza" token prefix olarak "yakazan"'da eşleşmez → TRAFIK_KAZASI olmamalı
        assertThat(type).isNotEqualTo(NewsType.TRAFIK_KAZASI);
    }

    // ── Öncelik Sırası Testi ────────────────────────────────────────────

    @Test
    void shouldUsePriorityWhenScoresAreClose() {
        // Hem trafik kazası hem yangın sinyalleri taşıyan metin
        String text = "Kocaeli'de trafik kazası sonrası araç alev aldı, itfaiye müdahale etti. "
                     + "Çarpışma sonucu motosiklet yandı, ambulans olay yerine geldi.";

        NewsType type = classifier.classify(text);

        // TRAFIK_KAZASI öncelikli
        assertThat(type).isEqualTo(NewsType.TRAFIK_KAZASI);
    }

    // ── Spor / Etkinlik Testi ───────────────────────────────────────────

    @Test
    void shouldClassifySportsNewsAsCulturalEvent() {
        String text = "Kocaelispor yeni transferini açıkladı. Forvet oyuncu imza attı.";

        NewsType type = classifier.classify(text);

        assertThat(type).isEqualTo(NewsType.KULTUREL_ETKINLIK);
    }
}
