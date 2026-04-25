package com.locnguyen.ecommerce.domains.brand.controller;

import com.locnguyen.ecommerce.common.constants.AppConstants;
import com.locnguyen.ecommerce.common.response.ApiResponse;
import com.locnguyen.ecommerce.common.response.PagedResponse;
import com.locnguyen.ecommerce.domains.brand.dto.BrandFilter;
import com.locnguyen.ecommerce.domains.brand.dto.BrandResponse;
import com.locnguyen.ecommerce.domains.brand.dto.CreateBrandRequest;
import com.locnguyen.ecommerce.domains.brand.dto.UpdateBrandRequest;
import com.locnguyen.ecommerce.domains.brand.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Brand", description = "Product brand management")
@RestController
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @Operation(summary = "List active brands")
    @GetMapping(AppConstants.API_V1 + "/brands")
    public ApiResponse<List<BrandResponse>> listBrands() {
        return ApiResponse.success(brandService.getActiveBrands());
    }

    @Operation(summary = "Get brand by ID")
    @GetMapping(AppConstants.API_V1 + "/brands/{id}")
    public ApiResponse<BrandResponse> getBrand(@PathVariable Long id) {
        return ApiResponse.success(brandService.getBrandById(id));
    }

    @Operation(summary = "[Admin] List all brands with filter and pagination")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STAFF')")
    @GetMapping(AppConstants.API_V1 + "/admin/brands")
    public ApiResponse<PagedResponse<BrandResponse>> getBrands(
            @ModelAttribute BrandFilter filter,
            @PageableDefault(size = AppConstants.DEFAULT_PAGE_SIZE, sort = "sortOrder") Pageable pageable) {
        return ApiResponse.success(brandService.getBrands(filter, pageable));
    }

    @Operation(summary = "[Admin] Create brand")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(AppConstants.API_V1 + "/admin/brands")
    public ApiResponse<BrandResponse> createBrand(
            @Valid @RequestBody CreateBrandRequest request) {
        return ApiResponse.created(brandService.createBrand(request));
    }

    @Operation(summary = "[Admin] Update brand")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping(AppConstants.API_V1 + "/admin/brands/{id}")
    public ApiResponse<BrandResponse> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBrandRequest request) {
        return ApiResponse.success(brandService.updateBrand(id, request));
    }

    @Operation(summary = "[Admin] Delete brand (soft)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping(AppConstants.API_V1 + "/admin/brands/{id}")
    public ApiResponse<Void> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ApiResponse.noContent();
    }
}
