package com.locnguyen.ecommerce.domains.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Payment callback request — from payment gateway")
public class PaymentCallbackRequest {

    @NotBlank(message = "Order code is required")
    @Schema(example = "ORD20260408123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderCode;

    @NotBlank(message = "Transaction status is required")
    @Schema(example = "SUCCESS", description = "SUCCESS or FAILED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(example = "TXN_GW_12345", description = "Provider's transaction ID")
    private String providerTxnId;

    @Schema(example = "VNPAY", description = "Payment provider identifier")
    private String provider;

    @Schema(description = "Raw callback payload from gateway (JSON)")
    private String payload;

    /**
     * HMAC or digital signature supplied by the payment gateway.
     *
     * <p>This field is optional at the DTO level because different gateways
     * deliver the signature via different channels (request body field, query
     * parameter, or HTTP header). The actual verification logic must be
     * implemented per-gateway in a dedicated verifier when gateway integration
     * is added. See the TODO in {@code PaymentServiceImpl.processCallback}.
     */
    @Schema(description = "Gateway-supplied HMAC or signature for request authenticity verification")
    private String signature;
}
