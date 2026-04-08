package com.locnguyen.ecommerce.domains.product.dto;

import com.locnguyen.ecommerce.domains.product.enums.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Filter parameters for product list queries.
 * All fields are optional — null means "no filter on this field".
 */
@Getter
@Builder
@Schema(description = "Product list filters")
public class ProductFilter {

    @Schema(description = "Search keyword (matches product name)", example = "áo thun")
    private String keyword;

    @Schema(description = "Filter by category ID", example = "1")
    private Long categoryId;

    @Schema(description = "Filter by brand ID", example = "2")
    private Long brandId;

    @Schema(description = "Filter by product status", example = "PUBLISHED")
    private ProductStatus status;

    @Schema(description = "Minimum price (variant sale or base price)", example = "50000")
    private BigDecimal minPrice;

    @Schema(description = "Maximum price (variant sale or base price)", example = "500000")
    private BigDecimal maxPrice;

    @Schema(description = "Show only featured products", example = "false")
    private Boolean featured;
}
