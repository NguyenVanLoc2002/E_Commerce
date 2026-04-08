package com.locnguyen.ecommerce.domains.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Product attribute value (e.g., Color=Red, Size=M)")
public class AttributeResponse {

    private final String name;
    private final String value;
}
