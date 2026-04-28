package com.locnguyen.ecommerce.domains.inventory.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;
@Getter
@Setter
@NoArgsConstructor
@ToString
public class InventoryFilter {

    private UUID variantId;
    private UUID warehouseId;
    private UUID productId;

    private String sku;
    private String keyword;
    private String variantStatus;

    private Boolean outOfStock;
    private Boolean lowStock;
    private Integer lowStockThreshold;
}