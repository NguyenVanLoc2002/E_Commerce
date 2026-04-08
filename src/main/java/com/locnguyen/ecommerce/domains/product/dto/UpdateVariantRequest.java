package com.locnguyen.ecommerce.domains.product.dto;

import com.locnguyen.ecommerce.domains.productvariant.enums.ProductVariantStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "Update variant request — only provided fields are updated")
public class UpdateVariantRequest {

    @Size(max = 100)
    private String sku;

    @Size(max = 100)
    private String barcode;

    @Size(max = 255)
    private String variantName;

    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private BigDecimal compareAtPrice;

    private Integer weightGram;

    private ProductVariantStatus status;

    @Schema(description = "Replace attribute values with this list")
    private List<AttributeRequest> attributes;
}
