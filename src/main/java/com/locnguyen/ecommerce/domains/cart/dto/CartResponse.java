package com.locnguyen.ecommerce.domains.cart.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Cart response — full cart with items and totals")
public class CartResponse {

    private final Long id;
    private final List<CartItemResponse> items;
    private final int totalItems;
    private final BigDecimal subTotal;
    private final LocalDateTime updatedAt;
}
