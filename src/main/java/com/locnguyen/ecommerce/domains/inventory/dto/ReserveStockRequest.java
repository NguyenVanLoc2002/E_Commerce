package com.locnguyen.ecommerce.domains.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Reserve stock request — hold stock for an order")
public class ReserveStockRequest {

    @NotNull(message = "Variant ID is required")
    @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long variantId;

    @NotNull(message = "Warehouse ID is required")
    @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long warehouseId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    @Schema(example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    @Schema(example = "ORDER", description = "Reference type (e.g., ORDER)")
    private String referenceType;

    @Schema(example = "ORD202604080001", description = "Reference ID (e.g., order code)")
    private String referenceId;

    @Schema(description = "Expiration time for this reservation (ISO-8601)")
    private LocalDateTime expiresAt;
}
