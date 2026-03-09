package com.bedirhan.cityeventmonitor.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocationExtractorTest {

    private final LocationExtractor extractor = new LocationExtractor();

    @Test
    void shouldExtractDistrictAndKnownPlace() {
        String text = "Kocaeli'nin İzmit ilçesinde Yahyakaptan Mahallesi D-100 Karayolu üzerinde trafik kazası meydana geldi.";

        LocationResult result = extractor.extract(text);

        assertThat(result.getDistrict()).isEqualTo("Izmit");
        assertThat(result.getLocationText()).contains("yahyakaptan mahallesi").or().contains("d-100 karayolu");
    }

    @Test
    void shouldExtractOnlyDistrictWhenNoSpecificPlace() {
        String text = "Gebze ilçesinde sanayi sitesinde çıkan yangın paniğe neden oldu.";

        LocationResult result = extractor.extract(text);

        assertThat(result.getDistrict()).isEqualTo("Gebze");
        assertThat(result.getLocationText()).contains("sanayi sitesi");
    }

    @Test
    void shouldReturnNullsWhenNoLocation() {
        String text = "Bugün Kocaeli genelinde hava sıcaklıkları artmaya devam edecek.";

        LocationResult result = extractor.extract(text);

        assertThat(result.getDistrict()).isNull();
        assertThat(result.getLocationText()).isNull();
    }
}

