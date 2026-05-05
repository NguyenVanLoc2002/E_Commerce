package com.locnguyen.ecommerce.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchTextNormalizerTest {

    @Test
    void strips_vietnamese_accents_and_lowercases() {
        assertThat(SearchTextNormalizer.normalize("Áo Thun Nam"))
                .isEqualTo("ao thun nam");
        assertThat(SearchTextNormalizer.normalize("Quần Jean Slim"))
                .isEqualTo("quan jean slim");
    }

    @Test
    void maps_d_with_stroke_to_plain_d() {
        assertThat(SearchTextNormalizer.normalize("ĐỒNG hồ Đep"))
                .isEqualTo("dong ho dep");
        assertThat(SearchTextNormalizer.normalize("đậm đà"))
                .isEqualTo("dam da");
    }

    @Test
    void collapses_repeated_whitespace_and_trims() {
        assertThat(SearchTextNormalizer.normalize("  Áo   Thun  \t Nam "))
                .isEqualTo("ao thun nam");
    }

    @Test
    void empty_or_blank_returns_empty_string() {
        assertThat(SearchTextNormalizer.normalize(null)).isEmpty();
        assertThat(SearchTextNormalizer.normalize("   ")).isEmpty();
    }

    @Test
    void normalize_and_join_skips_blanks() {
        String result = SearchTextNormalizer.normalizeAndJoin("Áo", null, "  ", "Thun", "Nam");
        assertThat(result).isEqualTo("ao thun nam");
    }
}
