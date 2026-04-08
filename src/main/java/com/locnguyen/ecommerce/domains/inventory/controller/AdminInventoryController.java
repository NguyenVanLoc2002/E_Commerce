package com.locnguyen.ecommerce.domains.inventory.controller;

import com.locnguyen.ecommerce.common.constants.AppConstants;
import com.locnguyen.ecommerce.common.response.ApiResponse;
import com.locnguyen.ecommerce.common.response.PagedResponse;
import com.locnguyen.ecommerce.domains.inventory.dto.*;
import com.locnguyen.ecommerce.domains.inventory.entity.InventoryReservation;
import com.locnguyen.ecommerce.domains.inventory.service.InventoryService;
import com.locnguyen.ecommerce.domains.inventory.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Inventory", description = "Inventory and warehouse management")
@RestController
@RequiredArgsConstructor
public class AdminInventoryController {

    private final WarehouseService warehouseService;
    private final InventoryService inventoryService;

    // ─── Warehouse endpoints ─────────────────────────────────────────────────

    @Operation(summary = "[Admin] List all warehouses")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(AppConstants.API_V1 + "/admin/warehouses")
    public ApiResponse<List<WarehouseResponse>> listWarehouses() {
        return ApiResponse.success(warehouseService.getActiveWarehouses());
    }

    @Operation(summary = "[Admin] Get warehouse by ID")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(AppConstants.API_V1 + "/admin/warehouses/{id}")
    public ApiResponse<WarehouseResponse> getWarehouse(@PathVariable Long id) {
        return ApiResponse.success(warehouseService.getWarehouseById(id));
    }

    @Operation(summary = "[Admin] Create warehouse")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(AppConstants.API_V1 + "/admin/warehouses")
    public ApiResponse<WarehouseResponse> createWarehouse(
            @Valid @RequestBody CreateWarehouseRequest request) {
        return ApiResponse.created(warehouseService.createWarehouse(request));
    }

    @Operation(summary = "[Admin] Update warehouse")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping(AppConstants.API_V1 + "/admin/warehouses/{id}")
    public ApiResponse<WarehouseResponse> updateWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWarehouseRequest request) {
        return ApiResponse.success(warehouseService.updateWarehouse(id, request));
    }

    @Operation(summary = "[Admin] Delete warehouse (soft)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping(AppConstants.API_V1 + "/admin/warehouses/{id}")
    public ApiResponse<Void> deleteWarehouse(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return ApiResponse.noContent();
    }

    // ─── Inventory view endpoints ────────────────────────────────────────────

    @Operation(summary = "[Admin] Get inventory by variant ID")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(AppConstants.API_V1 + "/admin/inventories/variant/{variantId}")
    public ApiResponse<List<InventoryResponse>> getInventoryByVariant(@PathVariable Long variantId) {
        return ApiResponse.success(inventoryService.getInventoryByVariant(variantId));
    }

    @Operation(summary = "[Admin] Get inventory by warehouse ID")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(AppConstants.API_V1 + "/admin/inventories/warehouse/{warehouseId}")
    public ApiResponse<List<InventoryResponse>> getInventoryByWarehouse(@PathVariable Long warehouseId) {
        return ApiResponse.success(inventoryService.getInventoryByWarehouse(warehouseId));
    }

    @Operation(summary = "[Admin] Get inventory detail (variant + warehouse)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(AppConstants.API_V1 + "/admin/inventories/variant/{variantId}/warehouse/{warehouseId}")
    public ApiResponse<InventoryResponse> getInventoryDetail(
            @PathVariable Long variantId,
            @PathVariable Long warehouseId) {
        return ApiResponse.success(inventoryService.getInventoryDetail(variantId, warehouseId));
    }

    // ─── Stock operations ────────────────────────────────────────────────────

    @Operation(summary = "[Admin] Adjust stock (import, export, adjust, return)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(AppConstants.API_V1 + "/admin/inventories/adjust")
    public ApiResponse<StockMovementResponse> adjustStock(
            @Valid @RequestBody AdjustStockRequest request) {
        return ApiResponse.success(inventoryService.adjustStock(request));
    }

    @Operation(summary = "[Admin] Reserve stock for an order")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(AppConstants.API_V1 + "/admin/inventories/reserve")
    public ApiResponse<InventoryReservation> reserveStock(
            @Valid @RequestBody ReserveStockRequest request) {
        return ApiResponse.success(inventoryService.reserveStock(request));
    }

    @Operation(summary = "[Admin] Release reserved stock by reference")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(AppConstants.API_V1 + "/admin/inventories/release")
    public ApiResponse<Void> releaseStock(
            @RequestParam String referenceType,
            @RequestParam String referenceId) {
        inventoryService.releaseStock(referenceType, referenceId);
        return ApiResponse.noContent();
    }

    // ─── Stock movement history ──────────────────────────────────────────────

    @Operation(summary = "[Admin] Get stock movement history (filtered, paginated)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping(AppConstants.API_V1 + "/admin/inventories/movements")
    public ApiResponse<PagedResponse<StockMovementResponse>> getStockMovements(
            @Parameter(description = "Filter by variant ID") @RequestParam(required = false) Long variantId,
            @Parameter(description = "Filter by warehouse ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "Filter by movement type") @RequestParam(required = false) String movementType,
            Pageable pageable) {
        StockFilter filter = new StockFilter();
        filter.setVariantId(variantId);
        filter.setWarehouseId(warehouseId);
        filter.setMovementType(movementType);
        return ApiResponse.success(inventoryService.getStockMovements(filter, pageable));
    }
}
