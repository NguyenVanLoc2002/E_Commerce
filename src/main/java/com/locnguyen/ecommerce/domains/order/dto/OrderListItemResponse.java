package com.locnguyen.ecommerce.domains.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lightweight order summary for list views — no item details included.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Order list item response — summary for list views")
public class OrderListItemResponse {

    private final Long id;
    private final String orderCode;
    private final String status;
    private final String paymentMethod;
    private final String paymentStatus;
    private final int totalItems;
    private final BigDecimal totalAmount;
    private final LocalDateTime createdAt;
}
