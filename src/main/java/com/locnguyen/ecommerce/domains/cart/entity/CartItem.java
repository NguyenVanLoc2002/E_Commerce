package com.locnguyen.ecommerce.domains.cart.entity;

import com.locnguyen.ecommerce.common.auditing.BaseEntity;
import com.locnguyen.ecommerce.domains.productvariant.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A single line item in a cart, referencing a product variant with a quantity.
 *
 * <p>Unit price is NOT stored here — always read from the variant at checkout time
 * to guarantee the customer pays the current price.
 *
 * <p>Unique constraint on (cart_id, variant_id) prevents duplicate items.
 * The same variant can only appear once per cart.
 */
@Entity
@Table(name = "cart_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_cart_items_cart_variant",
                columnNames = {"cart_id", "variant_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"cart", "variant"})
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;
}
