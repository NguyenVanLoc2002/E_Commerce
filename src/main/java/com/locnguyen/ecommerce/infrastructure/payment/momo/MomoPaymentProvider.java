package com.locnguyen.ecommerce.infrastructure.payment.momo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.domains.order.entity.Order;
import com.locnguyen.ecommerce.domains.payment.config.MomoResolvedPaymentConfig;
import com.locnguyen.ecommerce.domains.payment.config.PaymentProviderConfigResolver;
import com.locnguyen.ecommerce.domains.payment.entity.Payment;
import com.locnguyen.ecommerce.domains.payment.provider.PaymentProvider;
import com.locnguyen.ecommerce.domains.payment.provider.PaymentProviderCreateResult;
import com.locnguyen.ecommerce.infrastructure.payment.PaymentRestClientFactory;
import com.locnguyen.ecommerce.infrastructure.payment.momo.dto.MomoCreatePaymentRequest;
import com.locnguyen.ecommerce.infrastructure.payment.momo.dto.MomoCreatePaymentResponse;
import com.locnguyen.ecommerce.infrastructure.payment.momo.dto.MomoIpnRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;

@Slf4j
@Component
public class MomoPaymentProvider implements PaymentProvider {

    static final String PROVIDER_NAME = "MOMO";

    private static final long MOMO_MIN_AMOUNT = 1_000L;
    private static final long MOMO_MAX_AMOUNT = 50_000_000L;
    private static final int MOMO_SUCCESS_CODE = 0;

    private final PaymentProviderConfigResolver configResolver;
    private final MomoSignatureService signatureService;
    private final ObjectMapper objectMapper;
    private final PaymentRestClientFactory restClientFactory;

    public MomoPaymentProvider(
            PaymentProviderConfigResolver configResolver,
            MomoSignatureService signatureService,
            ObjectMapper objectMapper,
            PaymentRestClientFactory restClientFactory) {
        this.configResolver = configResolver;
        this.signatureService = signatureService;
        this.objectMapper = objectMapper;
        this.restClientFactory = restClientFactory;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean verifySignature(String rawBody, String signature) {
        if (rawBody == null || rawBody.isBlank()) {
            return false;
        }
        try {
            MomoResolvedPaymentConfig config = momo();
            MomoIpnRequest ipn = objectMapper.readValue(rawBody, MomoIpnRequest.class);

            if (!config.partnerCode().equals(ipn.getPartnerCode())) {
                log.warn("MoMo IPN partnerCode mismatch: expected={} received={}",
                        config.partnerCode(), ipn.getPartnerCode());
                return false;
            }

            String receivedSignature = (signature != null && !signature.isBlank())
                    ? signature
                    : ipn.getSignature();
            if (receivedSignature == null || receivedSignature.isBlank()) {
                return false;
            }

            ipn.setSignature(receivedSignature);
            return signatureService.verifyIpnSignature(
                    config.accessKey(), config.secretKey(), ipn);
        } catch (IOException e) {
            log.warn("MoMo IPN signature verification failed â€” could not parse payload: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public BigDecimal extractAmount(String payload) {
        if (payload == null || payload.isBlank()) return null;
        try {
            JsonNode json = objectMapper.readTree(payload);
            JsonNode amountNode = json.get("amount");
            if (amountNode == null || amountNode.isNull()) return null;
            return BigDecimal.valueOf(amountNode.asLong());
        } catch (Exception e) {
            log.debug("MoMo extractAmount: failed to parse payload â€” {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isSuccess(String payload) {
        if (payload == null || payload.isBlank()) return false;
        try {
            JsonNode json = objectMapper.readTree(payload);
            JsonNode resultCode = json.get("resultCode");
            return resultCode != null && resultCode.asInt(-1) == MOMO_SUCCESS_CODE;
        } catch (Exception e) {
            log.debug("MoMo isSuccess: failed to parse payload â€” {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String extractProviderTxnId(String payload) {
        if (payload == null || payload.isBlank()) return null;
        try {
            JsonNode json = objectMapper.readTree(payload);
            JsonNode transId = json.get("transId");
            return (transId != null && !transId.isNull()) ? transId.asText() : null;
        } catch (Exception e) {
            log.debug("MoMo extractProviderTxnId: failed to parse payload â€” {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String extractOrderCode(String payload) {
        if (payload == null || payload.isBlank()) return null;
        try {
            JsonNode json = objectMapper.readTree(payload);
            JsonNode orderIdNode = json.get("orderId");
            if (orderIdNode == null || orderIdNode.isNull()) return null;
            return parseOrderCodeFromProviderOrderId(orderIdNode.asText());
        } catch (Exception e) {
            log.debug("MoMo extractOrderCode: failed to parse payload â€” {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String createPaymentUrl(Payment payment, Order order, String returnUrl, String callbackUrl) {
        return createPayment(payment, order, returnUrl, callbackUrl).getPaymentUrl();
    }

    @Override
    public PaymentProviderCreateResult createPayment(Payment payment, Order order,
                                                     String returnUrl, String callbackUrl) {
        MomoResolvedPaymentConfig config = momo();
        long amount = toMomoAmount(payment.getAmount());
        validateAmount(amount, order.getOrderCode());

        String providerOrderId = buildProviderOrderId(order.getOrderCode());
        String requestId = buildRequestId(payment.getPaymentCode());
        String orderInfo = "Thanh toan don hang " + order.getOrderCode();

        String rawIpnUrl = config.ipnUrl();
        String effectiveIpnUrl = (rawIpnUrl != null && !rawIpnUrl.isBlank())
                ? rawIpnUrl.trim()
                : callbackUrl;
        String effectiveRedirectUrl = returnUrl != null ? returnUrl.trim() : config.redirectUrl().trim();

        MomoCreatePaymentRequest requestBody = MomoCreatePaymentRequest.builder()
                .partnerCode(config.partnerCode())
                .accessKey(config.accessKey())
                .requestType(config.requestType())
                .ipnUrl(effectiveIpnUrl)
                .redirectUrl(effectiveRedirectUrl)
                .orderId(providerOrderId)
                .amount(amount)
                .orderInfo(orderInfo)
                .requestId(requestId)
                .extraData("")
                .lang(config.lang())
                .signature("")
                .build();

        String signature = signatureService.signCreatePaymentRequest(requestBody, config.secretKey());

        requestBody = MomoCreatePaymentRequest.builder()
                .partnerCode(requestBody.getPartnerCode())
                .accessKey(requestBody.getAccessKey())
                .requestType(requestBody.getRequestType())
                .ipnUrl(requestBody.getIpnUrl())
                .redirectUrl(requestBody.getRedirectUrl())
                .orderId(requestBody.getOrderId())
                .amount(requestBody.getAmount())
                .orderInfo(requestBody.getOrderInfo())
                .requestId(requestBody.getRequestId())
                .extraData(requestBody.getExtraData())
                .lang(requestBody.getLang())
                .signature(signature)
                .build();

        log.info("Calling MoMo create-payment API: providerOrderId={} requestId={} amount={} ipnUrl={} redirectUrl={}",
                providerOrderId, requestId, amount, effectiveIpnUrl, effectiveRedirectUrl);

        MomoCreatePaymentResponse momoResponse = callMomoCreateApi(config, requestBody, order.getOrderCode());

        if (momoResponse.getResultCode() != MOMO_SUCCESS_CODE) {
            log.warn("MoMo create-payment rejected: providerOrderId={} resultCode={} message={}",
                    providerOrderId, momoResponse.getResultCode(), momoResponse.getMessage());
            throw new AppException(ErrorCode.PAYMENT_FAILED,
                    "MoMo rejected payment creation: " + momoResponse.getMessage());
        }

        return PaymentProviderCreateResult.builder()
                .paymentUrl(momoResponse.getPayUrl())
                .deeplink(momoResponse.getDeeplink())
                .qrCodeUrl(momoResponse.getQrCodeUrl())
                .providerOrderId(providerOrderId)
                .providerRequestId(requestId)
                .resultCode(momoResponse.getResultCode())
                .message(momoResponse.getMessage())
                .build();
    }

    private MomoCreatePaymentResponse callMomoCreateApi(MomoResolvedPaymentConfig config,
                                                        MomoCreatePaymentRequest requestBody,
                                                        String orderCode) {
        try {
            RestClient restClient = restClientFactory.create(config.connectTimeoutMs(), config.readTimeoutMs());
            MomoCreatePaymentResponse response = restClient.post()
                    .uri(URI.create(config.createUrl()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(MomoCreatePaymentResponse.class);

            if (response == null) {
                throw new AppException(ErrorCode.PAYMENT_FAILED, "MoMo API returned empty response");
            }
            return response;
        } catch (AppException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("MoMo API call failed: orderCode={} error={}", orderCode, e.getMessage());
            throw new AppException(ErrorCode.PAYMENT_FAILED, "MoMo API call failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error calling MoMo API: orderCode={} error={}", orderCode, e.getMessage(), e);
            throw new AppException(ErrorCode.PAYMENT_FAILED, "Unexpected error calling MoMo API");
        }
    }

    private void validateAmount(long amount, String orderCode) {
        if (amount < MOMO_MIN_AMOUNT || amount > MOMO_MAX_AMOUNT) {
            log.warn("MoMo payment amount out of range: orderCode={} amount={}", orderCode, amount);
            throw new AppException(ErrorCode.PAYMENT_FAILED,
                    "Payment amount must be between " + MOMO_MIN_AMOUNT + " and " + MOMO_MAX_AMOUNT + " VND");
        }
    }

    private String buildProviderOrderId(String orderCode) {
        return "MOMO_" + orderCode + "_" + System.currentTimeMillis();
    }

    private String buildRequestId(String paymentCode) {
        return "REQ_" + paymentCode + "_" + System.currentTimeMillis();
    }

    static String parseOrderCodeFromProviderOrderId(String providerOrderId) {
        if (providerOrderId == null) return null;
        String withoutPrefix = providerOrderId.startsWith("MOMO_")
                ? providerOrderId.substring(5)
                : providerOrderId;
        int lastUnderscore = withoutPrefix.lastIndexOf('_');
        return lastUnderscore > 0
                ? withoutPrefix.substring(0, lastUnderscore)
                : withoutPrefix;
    }

    private static long toMomoAmount(BigDecimal amount) {
        return amount == null ? 0L : amount.longValue();
    }

    private MomoResolvedPaymentConfig momo() {
        return configResolver.resolveMomo();
    }
}
