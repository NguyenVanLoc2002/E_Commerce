package com.locnguyen.ecommerce.common.utils;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Normalizes free-form text for keyword search:
 * lowercase, trim, strip Vietnamese accents, collapse whitespace, map đ/Đ to d/D.
 *
 * <p>Used to build the denormalized {@code products.search_text} column and to
 * normalize incoming search keywords so that FULLTEXT queries are
 * case-insensitive and accent-insensitive.
 *
 * <p>Examples:
 * <pre>
 *   "Áo Thun  Nam"      → "ao thun nam"
 *   "ĐỒNG hồ"           → "dong ho"
 *   "  Quần Jean Slim " → "quan jean slim"
 * </pre>
 */
public final class SearchTextNormalizer {

    private SearchTextNormalizer() {}

    private static final Pattern DIACRITICS =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private static final Pattern WHITESPACE =
            Pattern.compile("\\s+");

    /** Normalize a single value for search. Returns "" for null/blank input. */
    public static String normalize(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return "";

        // đ/Đ are not decomposable in NFD, handle explicitly first.
        String mapped = trimmed.replace('đ', 'd').replace('Đ', 'D');

        String decomposed = Normalizer.normalize(mapped, Normalizer.Form.NFD);
        String stripped = DIACRITICS.matcher(decomposed).replaceAll("");
        String lower = stripped.toLowerCase();
        return WHITESPACE.matcher(lower).replaceAll(" ").trim();
    }

    /**
     * Join several values into a single normalized search string.
     * Null/blank values are skipped. Result is space-separated, deduplicated by whitespace.
     */
    public static String normalizeAndJoin(String... parts) {
        if (parts == null || parts.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            String n = normalize(part);
            if (n.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(n);
        }
        return sb.toString();
    }
}
