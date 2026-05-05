package com.locnguyen.ecommerce.domains.product.service;

import com.locnguyen.ecommerce.common.response.PagedResponse;
import com.locnguyen.ecommerce.domains.product.dto.attribute.CreateProductAttributeRequest;
import com.locnguyen.ecommerce.domains.product.dto.attribute.CreateProductAttributeValueRequest;
import com.locnguyen.ecommerce.domains.product.dto.attribute.ProductAttributeFilter;
import com.locnguyen.ecommerce.domains.product.dto.attribute.ProductAttributeResponse;
import com.locnguyen.ecommerce.domains.product.dto.attribute.ProductAttributeValueResponse;
import com.locnguyen.ecommerce.domains.product.dto.attribute.UpdateProductAttributeRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductAttributeService {

    PagedResponse<ProductAttributeResponse> getAttributes(ProductAttributeFilter filter, Pageable pageable);

    ProductAttributeResponse getAttribute(UUID id);

    ProductAttributeResponse createAttribute(CreateProductAttributeRequest request);

    ProductAttributeResponse updateAttribute(UUID id, UpdateProductAttributeRequest request);

    void deleteAttribute(UUID id);

    ProductAttributeValueResponse addValue(UUID attributeId, CreateProductAttributeValueRequest request);

    ProductAttributeValueResponse updateValue(UUID attributeId, UUID valueId, CreateProductAttributeValueRequest request);

    void deleteValue(UUID attributeId, UUID valueId);
}
