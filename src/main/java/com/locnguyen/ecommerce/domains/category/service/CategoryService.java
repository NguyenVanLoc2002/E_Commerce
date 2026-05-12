package com.locnguyen.ecommerce.domains.category.service;

import com.locnguyen.ecommerce.common.response.PagedResponse;
import com.locnguyen.ecommerce.domains.category.dto.CategoryFilter;
import com.locnguyen.ecommerce.domains.category.dto.CategoryResponse;
import com.locnguyen.ecommerce.domains.category.dto.CreateCategoryRequest;
import com.locnguyen.ecommerce.domains.category.dto.UpdateCategoryRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    PagedResponse<CategoryResponse> getAllCategories(CategoryFilter filter, Pageable pageable);

    List<CategoryResponse> getActiveCategories();

    CategoryResponse getCategoryById(UUID id);

    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request);

    void deleteCategory(UUID id);
}
