package com.locnguyen.ecommerce.domains.productvariant.service;

import com.locnguyen.ecommerce.domains.product.dto.CreateVariantRequest;
import com.locnguyen.ecommerce.domains.product.dto.UpdateVariantRequest;
import com.locnguyen.ecommerce.domains.product.dto.VariantResponse;

import java.util.List;
import java.util.UUID;

public interface ProductVariantService {

    VariantResponse createVariant(UUID productId, CreateVariantRequest request);

    VariantResponse updateVariant(UUID productId, UUID variantId, UpdateVariantRequest request);

    void deleteVariant(UUID productId, UUID variantId);

    List<VariantResponse> getVariantsByProduct(UUID productId, Boolean isDeleted, Boolean includeDeleted);
}
