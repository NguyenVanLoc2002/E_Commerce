package com.locnguyen.ecommerce.infrastructure.payment.paypal;

import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.domains.payment.config.PaymentProviderConfigResolver;
import com.locnguyen.ecommerce.domains.payment.config.PaypalResolvedPaymentConfig;
import com.locnguyen.ecommerce.infrastructure.payment.PaymentRestClientFactory;
import com.locnguyen.ecommerce.infrastructure.payment.paypal.dto.PaypalAccessTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaypalOAuthClient {

    private static final int EXPIRY_BUFFER_SECONDS = 60;

    private final PaymentProviderConfigResolver configResolver;
    private final PaymentRestClientFactory restClientFactory;

    private volatile String cachedToken;
    private volatile Instant tokenExpiresAt = Instant.MIN;
    private volatile String cachedConfigFingerprint;

    public String getAccessToken() {
        PaypalResolvedPaymentConfig config = paypal();
        String fingerprint = fingerprint(config);
        if (cachedToken != null
                && Instant.now().isBefore(tokenExpiresAt)
                && fingerprint.equals(cachedConfigFingerprint)) {
            return cachedToken;
        }
        return refreshToken(config, fingerprint);
    }

    private synchronized String refreshToken(PaypalResolvedPaymentConfig config, String fingerprint) {
        if (cachedToken != null
                && Instant.now().isBefore(tokenExpiresAt)
                && fingerprint.equals(cachedConfigFingerprint)) {
            return cachedToken;
        }

        RestClient restClient = restClientFactory.create(config.connectTimeoutMs(), config.readTimeoutMs());
        String credentials = Base64.getEncoder().encodeToString(
                (config.clientId() + ":" + config.clientSecret()).getBytes(StandardCharsets.UTF_8));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");

        PaypalAccessTokenResponse tokenResponse;
        try {
            tokenResponse = restClient.post()
                    .uri(config.baseUrl() + "/v1/oauth2/token")
                    .header("Authorization", "Basic " + credentials)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(PaypalAccessTokenResponse.class);
        } catch (RestClientException e) {
            log.error("PayPal token endpoint call failed: error={}", e.getMessage());
            throw new AppException(ErrorCode.PAYMENT_FAILED,
                    "Failed to obtain PayPal access token: " + e.getMessage());
        }

        if (tokenResponse == null || tokenResponse.getAccessToken() == null
                || tokenResponse.getAccessToken().isBlank()) {
            throw new AppException(ErrorCode.PAYMENT_FAILED,
                    "PayPal returned an empty access token");
        }

        cachedToken = tokenResponse.getAccessToken();
        cachedConfigFingerprint = fingerprint;
        tokenExpiresAt = Instant.now()
                .plusSeconds(tokenResponse.getExpiresIn())
                .minusSeconds(EXPIRY_BUFFER_SECONDS);
        return cachedToken;
    }

    private PaypalResolvedPaymentConfig paypal() {
        return configResolver.resolvePaypal();
    }

    private String fingerprint(PaypalResolvedPaymentConfig config) {
        return config.baseUrl() + "|" + config.clientId() + "|" + config.clientSecret();
    }
}
