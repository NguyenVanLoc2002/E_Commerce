package com.locnguyen.ecommerce.domains.promotion.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.util.UUID;
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Record of a single voucher redemption")
public class VoucherUsageResponse {

    private final UUID id;
    private final UUID voucherId;
    private final String voucherCode;
    private final UUID customerId;
    private final UUID orderId;
    private final BigDecimal discountAmount;
    private final LocalDateTime usedAt;
}
