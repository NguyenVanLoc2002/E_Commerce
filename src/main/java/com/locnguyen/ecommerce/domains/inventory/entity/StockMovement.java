package com.locnguyen.ecommerce.domains.inventory.entity;

import com.locnguyen.ecommerce.common.auditing.BaseEntity;
import com.locnguyen.ecommerce.domains.inventory.enums.StockMovementType;
import com.locnguyen.ecommerce.domains.productvariant.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Immutable audit trail for every inventory change.
 *
 * <p>Records before/after values for full traceability of stock operations.
 * Extends {@link BaseEntity} (no soft delete) — movements are permanent audit records.
 */
@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"variant", "warehouse"})
public class StockMovement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", length = 50, nullable = false)
    private StockMovementType movementType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "before_on_hand", nullable = false)
    private Integer beforeOnHand = 0;

    @Column(name = "before_reserved", nullable = false)
    private Integer beforeReserved = 0;

    @Column(name = "before_available", nullable = false)
    private Integer beforeAvailable = 0;

    @Column(name = "after_on_hand", nullable = false)
    private Integer afterOnHand = 0;

    @Column(name = "after_reserved", nullable = false)
    private Integer afterReserved = 0;

    @Column(name = "after_available", nullable = false)
    private Integer afterAvailable = 0;
}
