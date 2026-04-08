package com.locnguyen.ecommerce.domains.brand.mapper;

import com.locnguyen.ecommerce.domains.brand.dto.BrandResponse;
import com.locnguyen.ecommerce.domains.brand.dto.UpdateBrandRequest;
import com.locnguyen.ecommerce.domains.brand.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BrandMapper {

    BrandResponse toResponse(Brand brand);

    void updateEntity(UpdateBrandRequest request, @MappingTarget Brand brand);
}
