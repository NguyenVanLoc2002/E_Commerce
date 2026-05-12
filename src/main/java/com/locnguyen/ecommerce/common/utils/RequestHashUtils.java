package com.locnguyen.ecommerce.common.utils;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Deterministic SHA-256 hashing for idempotency request payloads.
 *
 * <p>The hash covers only stable business fields — never timestamps, tokens, or headers.
 * Two semantically identical requests must produce the same hash regardless of when
 * they were sent. The hash is returned as a 64-character lowercase hex string.
 */
@Slf4j
public final class RequestHashUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private RequestHashUtils() {}

    /**
     * Hash any object by serializing it to sorted JSON then SHA-256.
     * Adds the {@code userId} as a prefix to bind the hash to the caller.
     */
    public static String hash(Object payload, UUID userId) {
        try {
            String json = MAPPER.writeValueAsString(payload);
            String raw = userId + ":" + json;
            return sha256Hex(raw);
        } catch (Exception e) {
            log.warn("RequestHashUtils.hash failed, falling back to simple hash: {}", e.getMessage());
            return sha256Hex(userId + ":" + payload.toString());
        }
    }

    /**
     * Hash a flat map of key-value pairs (for cases where the payload is assembled manually).
     */
    public static String hashFields(UUID userId, String... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("keyValuePairs must be even (key, value, key, value...)");
        }
        Map<String, String> fields = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            fields.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return hash(fields, userId);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
