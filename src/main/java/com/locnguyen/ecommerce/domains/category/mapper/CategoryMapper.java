package com.locnguyen.ecommerce.domains.category.mapper;

import com.locnguyen.ecommerce.domains.category.dto.CategoryResponse;
import com.locnguyen.ecommerce.domains.category.dto.UpdateCategoryRequest;
import com.locnguyen.ecommerce.domains.category.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);

    void updateEntity(UpdateCategoryRequest request, @MappingTarget Category category);
}
