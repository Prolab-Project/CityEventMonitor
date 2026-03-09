package com.bedirhan.cityeventmonitor.service;

import com.bedirhan.cityeventmonitor.model.NewsType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NewsTypeClassifierTest {

    private final NewsTypeClassifier classifier = new NewsTypeClassifier();

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
}

