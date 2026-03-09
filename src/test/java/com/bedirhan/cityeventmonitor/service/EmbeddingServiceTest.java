package com.bedirhan.cityeventmonitor.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingServiceTest {

    private final TextPreprocessor textPreprocessor = new TextPreprocessor();
    private final EmbeddingService embeddingService = new EmbeddingService(textPreprocessor);

    @Test
    void shouldGenerateTermFrequencyVector() {
        String title = "Trafik Kazası";
        String content = "İzmit d-100 karayolunda trafik kazası meydana geldi.";

        Map<CharSequence, Integer> vector = embeddingService.generateTermFrequencyVector(title, content);

        // Başlıktaki kelimelerin ağırlığı x2 (başlık işlenirken +1, content işlenirken +1 = toplam 2)
        // Eğer kelime hem title hem content'te geçiyorsa combined'da iki defa olur, title'dan da 1 defa gelir = 3.
        assertThat(vector).containsEntry("trafik", 3);
        assertThat(vector).containsEntry("kazası", 3);

        // Content'teki diğer kelimeler (1)
        assertThat(vector).containsEntry("izmit", 1);
        assertThat(vector).containsEntry("d-100", 1); // preprocessor keeps punctuation like dash
        assertThat(vector).containsEntry("karayolunda", 1);
        assertThat(vector).containsEntry("meydana", 1);
        assertThat(vector).containsEntry("geldi.", 1);
    }

    @Test
    void shouldFilterStopWords() {
        String title = "Ve veya ile";
        String content = "Bu bir test metnidir ve veya ile gibi kelimeler içerir.";

        Map<CharSequence, Integer> vector = embeddingService.generateTermFrequencyVector(title, content);

        assertThat(vector.containsKey("ve")).isFalse();
        assertThat(vector.containsKey("veya")).isFalse();
        assertThat(vector.containsKey("ile")).isFalse();
        assertThat(vector.containsKey("gibi")).isFalse();
        assertThat(vector.containsKey("bu")).isFalse(); // <= 2 letters
        
        assertThat(vector).containsKey("test");
        assertThat(vector).containsKey("metnidir");
        assertThat(vector).containsKey("kelimeler");
        assertThat(vector).containsKey("içerir.");
    }

    @Test
    void shouldHandleNullInputs() {
        Map<CharSequence, Integer> vector1 = embeddingService.generateTermFrequencyVector(null, "sadece içerik");
        assertThat(vector1).isNotEmpty();

        Map<CharSequence, Integer> vector2 = embeddingService.generateTermFrequencyVector("sadece başlık", null);
        assertThat(vector2).isNotEmpty();

        Map<CharSequence, Integer> vector3 = embeddingService.generateTermFrequencyVector(null, null);
        assertThat(vector3).isEmpty();
    }
}
