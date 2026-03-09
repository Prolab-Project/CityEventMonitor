package com.bedirhan.cityeventmonitor.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextPreprocessorTest {

    private final TextPreprocessor textPreprocessor = new TextPreprocessor();

    @Test
    void preprocess_shouldReturnEmptyString_whenInputIsNullOrBlank() {
        assertThat(textPreprocessor.preprocess(null)).isEqualTo("");
        assertThat(textPreprocessor.preprocess("   ")).isEqualTo("");
    }

    @Test
    void preprocess_shouldRemoveHtmlTagsAndNormalize() {
        String raw = "<p>Merhaba <strong>Kocaeli</strong>!</p>";

        String result = textPreprocessor.preprocess(raw);

        assertThat(result).isEqualTo("merhaba kocaeli!");
    }

    @Test
    void preprocess_shouldRemoveNoisePatterns() {
        String raw = "Kocaeli'de trafik kazası meydana geldi. Devamı için tıklayınız...";

        String result = textPreprocessor.preprocess(raw);

        assertThat(result).doesNotContain("devamı için tıklayınız");
        assertThat(result).contains("kocaeli'de trafik kazası meydana geldi");
    }

    @Test
    void preprocess_shouldCollapseWhitespacesAndLowercase() {
        String raw = "   Kocaeli   İzmit   TRAFİK   KAZASI   ";

        String result = textPreprocessor.preprocess(raw);

        assertThat(result).isEqualTo("kocaeli izmit trafik kazasi");
    }
}

