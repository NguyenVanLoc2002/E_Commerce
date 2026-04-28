package com.locnguyen.ecommerce.domains.inventory.dto;

import com.locnguyen.ecommerce.domains.inventory.enums.StockMovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;
@Data
@Schema(description = "Stock movement filter parameters")
public class StockFilter {

    @Schema(description = "Filter by variant ID")
    private UUID variantId;

    @Schema(description = "Filter by warehouse ID")
    private UUID warehouseId;

    @Schema(description = "Filter by movement type (IMPORT, EXPORT, ADJUSTMENT, RETURN)")
    private StockMovementType movementType;
}
