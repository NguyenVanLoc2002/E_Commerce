package com.locnguyen.ecommerce.domains.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Product media item")
public class MediaResponse {

    private final Long id;
    private final String mediaUrl;
    private final String mediaType;
    private final Integer sortOrder;
    private final boolean primary;
    private final Long variantId;
}
