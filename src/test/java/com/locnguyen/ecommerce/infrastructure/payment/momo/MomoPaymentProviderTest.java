package com.locnguyen.ecommerce.infrastructure.payment.momo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.domains.order.entity.Order;
import com.locnguyen.ecommerce.domains.payment.config.MomoResolvedPaymentConfig;
import com.locnguyen.ecommerce.domains.payment.config.PaymentProviderConfigResolver;
import com.locnguyen.ecommerce.domains.payment.entity.Payment;
import com.locnguyen.ecommerce.domains.payment.enums.PaymentRecordStatus;
import com.locnguyen.ecommerce.domains.payment.provider.PaymentProviderCreateResult;
import com.locnguyen.ecommerce.infrastructure.payment.PaymentRestClientFactory;
import com.locnguyen.ecommerce.infrastructure.payment.momo.dto.MomoCreatePaymentResponse;
import com.locnguyen.ecommerce.infrastructure.payment.momo.dto.MomoIpnRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MomoPaymentProviderTest {

    @Mock
    private PaymentProviderConfigResolver configResolver;
    @Mock
    private PaymentRestClientFactory restClientFactory;
    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestBodyUriSpec uriSpec;
    @Mock
    private RestClient.RequestBodySpec bodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MomoSignatureService signatureService = new MomoSignatureService();

    private MomoResolvedPaymentConfig momoConfig;
    private MomoPaymentProvider provider;

    @BeforeEach
    void setUp() {
        momoConfig = new MomoResolvedPaymentConfig(
                true,
                "TEST",
                "MOMOTEST01",
                "F8BBA842ECF85",
                "K951B6PE1waDMi640xX08PD3vg6EkVlz",
                "https://test-payment.momo.vn/v2/gateway/api/create",
                "http://localhost:5173/payment/momo/return",
                "https://example.com/api/v1/payments/webhooks/MOMO",
                "captureWallet",
                "vi",
                30_000,
                30_000
        );

        when(configResolver.resolveMomo()).thenReturn(momoConfig);
        when(restClientFactory.create(anyInt(), anyInt())).thenReturn(restClient);

        doReturn(uriSpec).when(restClient).post();
        doReturn(bodySpec).when(uriSpec).uri(any(URI.class));
        doReturn(bodySpec).when(bodySpec).contentType(any(MediaType.class));
        doReturn(bodySpec).when(bodySpec).body(any(Object.class));
        doReturn(responseSpec).when(bodySpec).retrieve();

        provider = new MomoPaymentProvider(configResolver, signatureService, objectMapper, restClientFactory);
    }

    private Payment payment(String paymentCode, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setPaymentCode(paymentCode);
        payment.setAmount(amount);
        return payment;
    }

    private Order order(String orderCode) {
        Order order = new Order();
        order.setOrderCode(orderCode);
        return order;
    }

    private MomoCreatePaymentResponse successResponse(String orderId, String payUrl) {
        MomoCreatePaymentResponse response = new MomoCreatePaymentResponse();
        response.setResultCode(0);
        response.setMessage("Successful.");
        response.setOrderId(orderId);
        response.setPayUrl(payUrl);
        response.setDeeplink("momo://pay?orderId=" + orderId);
        response.setQrCodeUrl("data:image/png;base64,qrdata");
        return response;
    }

    private MomoCreatePaymentResponse failureResponse(int code, String message) {
        MomoCreatePaymentResponse response = new MomoCreatePaymentResponse();
        response.setResultCode(code);
        response.setMessage(message);
        return response;
    }

    @Test
    void getProviderName_returnsMomo() {
        assertThat(provider.getProviderName()).isEqualTo("MOMO");
    }

    @Nested
    class CreatePayment {

        @Test
        void returnsPaymentUrl_whenMomoReturnsSuccess() {
            when(responseSpec.body(MomoCreatePaymentResponse.class))
                    .thenReturn(successResponse("MOMO_ORD001_123", "https://test-payment.momo.vn/pay?t=abc"));

            PaymentProviderCreateResult result = provider.createPayment(
                    payment("PAY001", new BigDecimal("50000")),
                    order("ORD001"),
                    "http://localhost:5173/return",
                    "https://example.com/webhooks/MOMO");

            assertThat(result.getPaymentUrl()).isEqualTo("https://test-payment.momo.vn/pay?t=abc");
        }

        @Test
        void usesResolvedTimeouts_whenCallingApi() {
            when(responseSpec.body(MomoCreatePaymentResponse.class))
                    .thenReturn(successResponse("MOMO_ORD001_123", "https://test-payment.momo.vn/pay?t=abc"));

            provider.createPayment(
                    payment("PAY001", new BigDecimal("50000")),
                    order("ORD001"),
                    "http://localhost:5173/return",
                    "https://example.com/webhooks/MOMO");

            verify(restClientFactory).create(30_000, 30_000);
        }

        @Test
        void fallsBackToResolvedIpnUrl_whenRequestCallbackIsBlank() {
            Object[] capturedBody = new Object[1];
            doReturn(bodySpec).when(bodySpec).body(any(Object.class));
            org.mockito.Mockito.doAnswer(invocation -> {
                capturedBody[0] = invocation.getArgument(0);
                return bodySpec;
            }).when(bodySpec).body(any(Object.class));
            when(responseSpec.body(MomoCreatePaymentResponse.class))
                    .thenReturn(successResponse("MOMO_ORD001_123", "https://test-payment.momo.vn/pay?t=abc"));

            provider.createPayment(
                    payment("PAY001", new BigDecimal("50000")),
                    order("ORD001"),
                    null,
                    " ");

            assertThat(capturedBody[0]).isInstanceOf(com.locnguyen.ecommerce.infrastructure.payment.momo.dto.MomoCreatePaymentRequest.class);
            com.locnguyen.ecommerce.infrastructure.payment.momo.dto.MomoCreatePaymentRequest request =
                    (com.locnguyen.ecommerce.infrastructure.payment.momo.dto.MomoCreatePaymentRequest) capturedBody[0];
            assertThat(request.getIpnUrl()).isEqualTo("https://example.com/api/v1/payments/webhooks/MOMO");
        }

        @Test
        void storesProviderIds_onSuccess() {
            when(responseSpec.body(MomoCreatePaymentResponse.class))
                    .thenReturn(successResponse("MOMO_ORD001_123", "https://pay.momo.vn/pay"));

            PaymentProviderCreateResult result = provider.createPayment(
                    payment("PAY001", new BigDecimal("50000")),
                    order("ORD001"),
                    "http://localhost:5173/return",
                    "https://example.com/webhooks/MOMO");

            assertThat(result.getProviderOrderId()).startsWith("MOMO_ORD001_");
            assertThat(result.getProviderRequestId()).startsWith("REQ_PAY001_");
            assertThat(result.getDeeplink()).startsWith("momo://");
            assertThat(result.getQrCodeUrl()).isNotBlank();
        }

        @Test
        void throwsPaymentFailed_whenResultCodeIsNotZero() {
            when(responseSpec.body(MomoCreatePaymentResponse.class))
                    .thenReturn(failureResponse(11, "Invalid access key"));

            assertThatThrownBy(() -> provider.createPayment(
                    payment("PAY001", new BigDecimal("50000")),
                    order("ORD001"),
                    "http://localhost:5173/return",
                    "https://example.com/webhooks/MOMO"))
                    .isInstanceOf(AppException.class)
                    .extracting(exception -> ((AppException) exception).getErrorCode())
                    .isEqualTo(ErrorCode.PAYMENT_FAILED);
        }

        @Test
        void throwsPaymentFailed_onHttpTimeout() {
            when(responseSpec.body(MomoCreatePaymentResponse.class))
                    .thenThrow(new RestClientException("Connection timed out"));

            assertThatThrownBy(() -> provider.createPayment(
                    payment("PAY001", new BigDecimal("50000")),
                    order("ORD001"),
                    "http://localhost:5173/return",
                    "https://example.com/webhooks/MOMO"))
                    .isInstanceOf(AppException.class)
                    .extracting(exception -> ((AppException) exception).getErrorCode())
                    .isEqualTo(ErrorCode.PAYMENT_FAILED);
        }

        @Test
        void throwsPaymentFailed_whenAmountBelowMinimum() {
            assertThatThrownBy(() -> provider.createPayment(
                    payment("PAY001", new BigDecimal("999")),
                    order("ORD001"),
                    "http://localhost:5173/return",
                    "https://example.com/webhooks/MOMO"))
                    .isInstanceOf(AppException.class)
                    .extracting(exception -> ((AppException) exception).getErrorCode())
                    .isEqualTo(ErrorCode.PAYMENT_FAILED);

            verify(restClientFactory, never()).create(anyInt(), anyInt());
            verify(restClient, never()).post();
        }

        @Test
        void throwsPaymentFailed_whenAmountExceedsMaximum() {
            assertThatThrownBy(() -> provider.createPayment(
                    payment("PAY001", new BigDecimal("50000001")),
                    order("ORD001"),
                    "http://localhost:5173/return",
                    "https://example.com/webhooks/MOMO"))
                    .isInstanceOf(AppException.class)
                    .extracting(exception -> ((AppException) exception).getErrorCode())
                    .isEqualTo(ErrorCode.PAYMENT_FAILED);

            verify(restClientFactory, never()).create(anyInt(), anyInt());
        }

        @Test
        void callsMomoApi_withJsonBody() {
            when(responseSpec.body(MomoCreatePaymentResponse.class))
                    .thenReturn(successResponse("MOMO_ORD001_123", "https://pay.momo.vn/pay"));

            provider.createPayment(
                    payment("PAY001", new BigDecimal("50000")),
                    order("ORD001"),
                    "http://localhost:5173/return",
                    "https://example.com/webhooks/MOMO");

            verify(bodySpec).contentType(MediaType.APPLICATION_JSON);
        }

        @Test
        void doesNotMarkPaymentPaid_afterSuccessfulCreate() {
            when(responseSpec.body(MomoCreatePaymentResponse.class))
                    .thenReturn(successResponse("MOMO_ORD001_123", "https://pay.momo.vn/pay"));

            Payment payment = payment("PAY001", new BigDecimal("50000"));
            PaymentRecordStatus statusBefore = payment.getStatus();

            provider.createPayment(
                    payment,
                    order("ORD001"),
                    "http://localhost:5173/return",
                    "https://example.com/webhooks/MOMO");

            assertThat(payment.getStatus()).isEqualTo(statusBefore);
        }
    }

    @Nested
    class CreatePaymentUrl {

        @Test
        void returnsPayUrl_fromMomoResponse() {
            when(responseSpec.body(MomoCreatePaymentResponse.class))
                    .thenReturn(successResponse("MOMO_ORD001_123", "https://test-payment.momo.vn/pay?t=xyz"));

            String url = provider.createPaymentUrl(
                    payment("PAY001", new BigDecimal("50000")),
                    order("ORD001"),
                    "http://localhost:5173/return",
                    "https://example.com/webhooks/MOMO");

            assertThat(url).isEqualTo("https://test-payment.momo.vn/pay?t=xyz");
        }
    }

    @Nested
    class IsSuccess {

        @Test
        void returnsTrue_whenResultCodeIsZero() {
            assertThat(provider.isSuccess("{\"resultCode\":0,\"message\":\"Successful.\"}")).isTrue();
        }

        @Test
        void returnsFalse_whenPayloadIsInvalid() {
            assertThat(provider.isSuccess("{\"resultCode\":11}")).isFalse();
            assertThat(provider.isSuccess("")).isFalse();
            assertThat(provider.isSuccess(null)).isFalse();
            assertThat(provider.isSuccess("not-json")).isFalse();
        }
    }

    @Nested
    class ExtractProviderTxnId {

        @Test
        void returnsTransId_fromValidIpnPayload() {
            assertThat(provider.extractProviderTxnId("{\"transId\":3455806203,\"resultCode\":0}"))
                    .isEqualTo("3455806203");
        }

        @Test
        void returnsNull_whenPayloadMissingOrInvalid() {
            assertThat(provider.extractProviderTxnId("{\"resultCode\":0}")).isNull();
            assertThat(provider.extractProviderTxnId(null)).isNull();
            assertThat(provider.extractProviderTxnId("not-json")).isNull();
        }
    }

    @Nested
    class ExtractOrderCode {

        @Test
        void parsesOrderCode_fromProviderOrderId() {
            String payload = "{\"orderId\":\"MOMO_ORD20260514123456_1715700000000\",\"resultCode\":0}";
            assertThat(provider.extractOrderCode(payload)).isEqualTo("ORD20260514123456");
        }

        @Test
        void returnsNull_whenPayloadMissingOrInvalid() {
            assertThat(provider.extractOrderCode("{\"resultCode\":0}")).isNull();
            assertThat(provider.extractOrderCode(null)).isNull();
        }
    }

    @Nested
    class ParseOrderCode {

        @Test
        void parsesCorrectly_fromFullProviderOrderId() {
            assertThat(MomoPaymentProvider.parseOrderCodeFromProviderOrderId("MOMO_ORD20260514123456_1715700000000"))
                    .isEqualTo("ORD20260514123456");
        }

        @Test
        void handlesMissingTimestamp_orNull() {
            assertThat(MomoPaymentProvider.parseOrderCodeFromProviderOrderId("MOMO_ORD20260514123456"))
                    .isEqualTo("ORD20260514123456");
            assertThat(MomoPaymentProvider.parseOrderCodeFromProviderOrderId(null)).isNull();
        }
    }

    @Nested
    class VerifySignature {

        private MomoIpnRequest buildValidIpn() {
            MomoIpnRequest ipn = new MomoIpnRequest();
            ipn.setPartnerCode("MOMOTEST01");
            ipn.setOrderId("MOMO_ORD001_1715700000000");
            ipn.setRequestId("REQ_PAY001_1715700000000");
            ipn.setAmount(50_000L);
            ipn.setOrderInfo("Thanh toan don hang ORD001");
            ipn.setOrderType("MOMO_WALLET");
            ipn.setTransId(3455806203L);
            ipn.setResultCode(0);
            ipn.setMessage("Successful.");
            ipn.setPayType("wallet");
            ipn.setResponseTime(1715700000000L);
            ipn.setExtraData("");
            return ipn;
        }

        private String toJson(MomoIpnRequest ipn) throws Exception {
            return objectMapper.writeValueAsString(ipn);
        }

        @Test
        void returnsFalse_whenRawBodyMissingOrMalformed() {
            assertThat(provider.verifySignature(null, "some-sig")).isFalse();
            assertThat(provider.verifySignature(" ", "some-sig")).isFalse();
            assertThat(provider.verifySignature("not-json", "sig")).isFalse();
        }

        @Test
        void returnsFalse_whenSignatureMissing() throws Exception {
            MomoIpnRequest ipn = buildValidIpn();
            ipn.setSignature(null);

            assertThat(provider.verifySignature(toJson(ipn), null)).isFalse();
        }

        @Test
        void returnsFalse_whenPartnerCodeMismatch() throws Exception {
            MomoIpnRequest ipn = buildValidIpn();
            ipn.setPartnerCode("WRONG_PARTNER");
            ipn.setSignature("any-sig");

            assertThat(provider.verifySignature(toJson(ipn), null)).isFalse();
        }

        @Test
        void returnsFalse_whenSignatureIsWrong() throws Exception {
            MomoIpnRequest ipn = buildValidIpn();
            ipn.setSignature("0000000000000000000000000000000000000000000000000000000000000000");

            assertThat(provider.verifySignature(toJson(ipn), null)).isFalse();
        }

        @Test
        void returnsTrue_whenValidSignatureEmbeddedInBody() throws Exception {
            MomoIpnRequest ipn = buildValidIpn();
            String signature = signatureService.signIpnRequest(momoConfig.accessKey(), momoConfig.secretKey(), ipn);
            ipn.setSignature(signature);

            assertThat(provider.verifySignature(toJson(ipn), null)).isTrue();
        }

        @Test
        void prefersHeaderSignature_whenProvided() throws Exception {
            MomoIpnRequest ipn = buildValidIpn();
            String signature = signatureService.signIpnRequest(momoConfig.accessKey(), momoConfig.secretKey(), ipn);
            ipn.setSignature("wrong-body-signature");

            assertThat(provider.verifySignature(toJson(ipn), signature)).isTrue();
        }
    }

    @Nested
    class ExtractAmount {

        @Test
        void returnsAmount_fromValidPayload() {
            assertThat(provider.extractAmount("{\"partnerCode\":\"MOMOTEST01\",\"amount\":50000,\"resultCode\":0}"))
                    .isEqualByComparingTo(new BigDecimal("50000"));
        }

        @Test
        void returnsNull_whenPayloadMissingOrInvalid() {
            assertThat(provider.extractAmount("{\"partnerCode\":\"MOMOTEST01\",\"resultCode\":0}")).isNull();
            assertThat(provider.extractAmount(null)).isNull();
            assertThat(provider.extractAmount("  ")).isNull();
            assertThat(provider.extractAmount("not-json")).isNull();
        }

        @Test
        void returnsZero_whenAmountIsZero() {
            assertThat(provider.extractAmount("{\"amount\":0,\"resultCode\":0}"))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
