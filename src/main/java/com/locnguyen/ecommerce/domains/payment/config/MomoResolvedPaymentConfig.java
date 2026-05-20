package com.locnguyen.ecommerce.domains.payment.config;

public record MomoResolvedPaymentConfig(
        boolean enabled,
        String environment,
        String partnerCode,
        String accessKey,
        String secretKey,
        String createUrl,
        String redirectUrl,
        String ipnUrl,
        String requestType,
        String lang,
        int connectTimeoutMs,
        int readTimeoutMs
) {
}
