package com.locnguyen.ecommerce.domains.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

import java.util.UUID;
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Inventory response — stock levels for a variant in a warehouse")
public class InventoryResponse {

    private final UUID id;
    private final UUID variantId;
    private final String variantName;
    private final String sku;
    private final UUID warehouseId;
    private final String warehouseName;
    private final int onHand;
    private final int reserved;
    private final int available;
    private final LocalDateTime updatedAt;
}
