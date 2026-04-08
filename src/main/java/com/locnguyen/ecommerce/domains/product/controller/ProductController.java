package com.locnguyen.ecommerce.domains.product.controller;

import com.locnguyen.ecommerce.common.constants.AppConstants;
import com.locnguyen.ecommerce.common.response.ApiResponse;
import com.locnguyen.ecommerce.domains.product.dto.*;
import com.locnguyen.ecommerce.domains.product.service.ProductService;
import com.locnguyen.ecommerce.domains.productvariant.service.ProductVariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Product", description = "Product catalog endpoints")
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductVariantService variantService;

    // ─── Public endpoints ─────────────────────────────────────────────────────

    @Operation(summary = "List published products with filters and search")
    @GetMapping(AppConstants.API_V1 + "/products")
    public ApiResponse<com.locnguyen.ecommerce.common.response.PagedResponse<ProductListItemResponse>> getProducts(
            ProductFilter filter,
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable) {
        return ApiResponse.success(productService.getPublishedProducts(filter, pageable));
    }

    @Operation(summary = "Get published product detail with variants and media")
    @GetMapping(AppConstants.API_V1 + "/products/{id}")
    public ApiResponse<ProductDetailResponse> getProduct(@PathVariable Long id) {
        return ApiResponse.success(productService.getProductById(id));
    }

    // ─── Admin endpoints ──────────────────────────────────────────────────────

    @Operation(summary = "[Admin] List all products (including drafts)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(AppConstants.API_V1 + "/admin/products")
    public ApiResponse<com.locnguyen.ecommerce.common.response.PagedResponse<ProductListItemResponse>> getAllProducts(
            ProductFilter filter,
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable) {
        return ApiResponse.success(productService.getAllProducts(filter, pageable));
    }

    @Operation(summary = "[Admin] Get any product detail by ID")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(AppConstants.API_V1 + "/admin/products/{id}")
    public ApiResponse<ProductDetailResponse> getProductDetailAdmin(@PathVariable Long id) {
        return ApiResponse.success(productService.getProductDetailAdmin(id));
    }

    @Operation(summary = "[Admin] Create product")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(AppConstants.API_V1 + "/admin/products")
    public ApiResponse<ProductDetailResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.created(productService.createProduct(request));
    }

    @Operation(summary = "[Admin] Update product")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping(AppConstants.API_V1 + "/admin/products/{id}")
    public ApiResponse<ProductDetailResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ApiResponse.success(productService.updateProduct(id, request));
    }

    @Operation(summary = "[Admin] Delete product (soft)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping(AppConstants.API_V1 + "/admin/products/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ApiResponse.noContent();
    }

    // ─── Variant endpoints ─────────────────────────────────────────────────────

    @Operation(summary = "[Admin] List variants for a product")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(AppConstants.API_V1 + "/admin/products/{productId}/variants")
    public ApiResponse<List<VariantResponse>> getVariants(@PathVariable Long productId) {
        return ApiResponse.success(variantService.getVariantsByProduct(productId));
    }

    @Operation(summary = "[Admin] Create variant for a product")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(AppConstants.API_V1 + "/admin/products/{productId}/variants")
    public ApiResponse<VariantResponse> createVariant(
            @PathVariable Long productId,
            @Valid @RequestBody CreateVariantRequest request) {
        return ApiResponse.created(variantService.createVariant(productId, request));
    }

    @Operation(summary = "[Admin] Update variant")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping(AppConstants.API_V1 + "/admin/products/{productId}/variants/{variantId}")
    public ApiResponse<VariantResponse> updateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody UpdateVariantRequest request) {
        return ApiResponse.success(variantService.updateVariant(productId, variantId, request));
    }

    @Operation(summary = "[Admin] Delete variant (soft)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping(AppConstants.API_V1 + "/admin/products/{productId}/variants/{variantId}")
    public ApiResponse<Void> deleteVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        variantService.deleteVariant(productId, variantId);
        return ApiResponse.noContent();
    }
}
