package com.locnguyen.ecommerce.domains.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;
@Data
@Schema(description = "Add item to cart request")
public class AddCartItemRequest {

    @NotNull(message = "Variant ID is required")
    @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID variantId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;
}
