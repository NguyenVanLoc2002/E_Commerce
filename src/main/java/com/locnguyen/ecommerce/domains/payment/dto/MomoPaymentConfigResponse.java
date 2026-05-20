package com.locnguyen.ecommerce.domains.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MomoPaymentConfigResponse {
    private final String provider;
    private final boolean managedInDatabase;
    private final boolean enabled;
    private final String environment;
    private final boolean hasPartnerCode;
    private final boolean hasAccessKey;
    private final boolean hasSecretKey;
    private final String createUrl;
    private final String redirectUrl;
    private final String ipnUrl;
    private final String requestType;
    private final String lang;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
}
