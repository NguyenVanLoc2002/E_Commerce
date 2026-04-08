package com.locnguyen.ecommerce.domains.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Attribute value — attached to a variant")
public class AttributeRequest {

    @NotBlank(message = "Attribute name is required")
    @Size(max = 100)
    @Schema(example = "Color", requiredMode = Schema.RequiredMode.REQUIRED)
    private String attributeName;

    @NotBlank(message = "Attribute value is required")
    @Size(max = 100)
    @Schema(example = "Trắng", requiredMode = Schema.RequiredMode.REQUIRED)
    private String value;
}
