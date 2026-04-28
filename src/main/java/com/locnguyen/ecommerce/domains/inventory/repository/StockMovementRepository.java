package com.locnguyen.ecommerce.domains.inventory.repository;

import com.locnguyen.ecommerce.domains.inventory.entity.StockMovement;
import com.locnguyen.ecommerce.domains.inventory.enums.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.UUID;
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    List<StockMovement> findByVariantIdOrderByCreatedAtDesc(UUID variantId);

    List<StockMovement> findByWarehouseIdOrderByCreatedAtDesc(UUID warehouseId);

    Page<StockMovement> findByVariantId(UUID variantId, Pageable pageable);

    Page<StockMovement> findByWarehouseId(UUID warehouseId, Pageable pageable);

    List<StockMovement> findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(
            String referenceType, String referenceId);

    Page<StockMovement> findByMovementType(StockMovementType movementType, Pageable pageable);

    @Query("SELECT sm FROM StockMovement sm " +
            "WHERE (:variantId IS NULL OR sm.variant.id = :variantId) " +
            "AND (:warehouseId IS NULL OR sm.warehouse.id = :warehouseId) " +
            "AND (:movementType IS NULL OR sm.movementType = :movementType) " +
            "ORDER BY sm.createdAt DESC")
    Page<StockMovement> filter(
            @Param("variantId") UUID variantId,
            @Param("warehouseId") UUID warehouseId,
            @Param("movementType") StockMovementType movementType,
            Pageable pageable);
}
