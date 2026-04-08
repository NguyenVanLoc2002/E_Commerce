package com.locnguyen.ecommerce.domains.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Product variant with attributes")
public class VariantResponse {

    private final Long id;
    private final String sku;
    private final String barcode;
    private final String variantName;
    private final BigDecimal basePrice;
    private final BigDecimal salePrice;
    private final BigDecimal compareAtPrice;
    private final Integer weightGram;
    private final String status;
    private final List<AttributeResponse> attributes;
}
