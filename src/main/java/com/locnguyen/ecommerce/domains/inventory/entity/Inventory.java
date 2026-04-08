package com.locnguyen.ecommerce.domains.inventory.entity;

import com.locnguyen.ecommerce.common.auditing.BaseEntity;
import com.locnguyen.ecommerce.domains.productvariant.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Tracks stock levels per variant per warehouse.
 *
 * <p>{@code available} is NOT a stored column — always computed as {@code onHand - reserved}.
 * This prevents data inconsistency between the three values.
 *
 * <p>Extends {@link BaseEntity} (no soft delete) because inventory records
 * should not be silently hidden — they represent real stock counts.
 */
@Entity
@Table(name = "inventories",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_inventories_variant_warehouse",
                columnNames = {"variant_id", "warehouse_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"variant", "warehouse"})
public class Inventory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "on_hand", nullable = false)
    private Integer onHand = 0;

    @Column(name = "reserved", nullable = false)
    private Integer reserved = 0;

    /**
     * Computed available stock. Never persisted.
     */
    @Transient
    public int getAvailable() {
        return onHand - reserved;
    }
}
