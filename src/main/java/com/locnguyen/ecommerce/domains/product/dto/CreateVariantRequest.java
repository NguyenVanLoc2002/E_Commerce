package com.locnguyen.ecommerce.domains.product.dto;

import com.locnguyen.ecommerce.domains.productvariant.enums.ProductVariantStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "Create product variant request")
public class CreateVariantRequest {

    @NotBlank(message = "SKU is required")
    @Size(max = 100)
    @Schema(example = "ATBN-WH-M", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sku;

    @Size(max = 100)
    @Schema(example = "9876543210")
    private String barcode;

    @NotBlank(message = "Variant name is required")
    @Size(max = 255)
    @Schema(example = "Trắng / M", requiredMode = Schema.RequiredMode.REQUIRED)
    private String variantName;

    @NotNull(message = "Base price is required")
    @Positive(message = "Base price must be greater than 0")
    @Schema(example = "200000", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal basePrice;

    @Positive(message = "Sale price must be greater than 0")
    @Schema(example = "150000")
    private BigDecimal salePrice;

    @Positive
    @Schema(example = "250000")
    private BigDecimal compareAtPrice;

    @Schema(example = "200")
    private Integer weightGram;

    private ProductVariantStatus status;

    @Schema(description = "Attribute values — e.g., [{attributeName: 'Color', value: 'Trắng'}]")
    private List<AttributeRequest> attributes;
}
