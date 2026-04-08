package com.locnguyen.ecommerce.domains.category.controller;

import com.locnguyen.ecommerce.common.constants.AppConstants;
import com.locnguyen.ecommerce.common.response.ApiResponse;
import com.locnguyen.ecommerce.domains.category.dto.CategoryResponse;
import com.locnguyen.ecommerce.domains.category.dto.CreateCategoryRequest;
import com.locnguyen.ecommerce.domains.category.dto.UpdateCategoryRequest;
import com.locnguyen.ecommerce.domains.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Category", description = "Product category management")
@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // ─── Public endpoints ─────────────────────────────────────────────────────

    @Operation(summary = "List active categories")
    @GetMapping(AppConstants.API_V1 + "/categories")
    public ApiResponse<List<CategoryResponse>> listCategories() {
        return ApiResponse.success(categoryService.getActiveCategories());
    }

    @Operation(summary = "Get category by ID")
    @GetMapping(AppConstants.API_V1 + "/categories/{id}")
    public ApiResponse<CategoryResponse> getCategory(@PathVariable Long id) {
        return ApiResponse.success(categoryService.getCategoryById(id));
    }

    // ─── Admin endpoints (path-protected by SecurityConfig) ──────────────────

    @Operation(summary = "[Admin] Create category")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(AppConstants.API_V1 + "/admin/categories")
    public ApiResponse<CategoryResponse> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.created(categoryService.createCategory(request));
    }

    @Operation(summary = "[Admin] Update category")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping(AppConstants.API_V1 + "/admin/categories/{id}")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.success(categoryService.updateCategory(id, request));
    }

    @Operation(summary = "[Admin] Delete category (soft)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping(AppConstants.API_V1 + "/admin/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ApiResponse.noContent();
    }
}
