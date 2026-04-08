package com.locnguyen.ecommerce.domains.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Update cart item quantity request")
public class UpdateCartItemRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;
}
