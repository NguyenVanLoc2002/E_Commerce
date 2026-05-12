package com.locnguyen.ecommerce.domains.brand.service;

import com.locnguyen.ecommerce.common.response.PagedResponse;
import com.locnguyen.ecommerce.domains.brand.dto.BrandFilter;
import com.locnguyen.ecommerce.domains.brand.dto.BrandResponse;
import com.locnguyen.ecommerce.domains.brand.dto.CreateBrandRequest;
import com.locnguyen.ecommerce.domains.brand.dto.UpdateBrandRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface BrandService {

    PagedResponse<BrandResponse> getBrands(BrandFilter filter, Pageable pageable);

    List<BrandResponse> getActiveBrands();

    BrandResponse getBrandById(UUID id);

    BrandResponse createBrand(CreateBrandRequest request);

    BrandResponse updateBrand(UUID id, UpdateBrandRequest request);

    void deleteBrand(UUID id);
}
