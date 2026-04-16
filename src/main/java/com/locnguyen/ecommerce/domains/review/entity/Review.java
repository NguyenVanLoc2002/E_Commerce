package com.locnguyen.ecommerce.domains.review.entity;

import com.locnguyen.ecommerce.common.auditing.BaseEntity;
import com.locnguyen.ecommerce.domains.customer.entity.Customer;
import com.locnguyen.ecommerce.domains.order.entity.OrderItem;
import com.locnguyen.ecommerce.domains.product.entity.Product;
import com.locnguyen.ecommerce.domains.review.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Verified-purchase product review.
 *
 * <p>A review is only allowed when the customer's order that contains the
 * reviewed product has reached {@code COMPLETED} status. This is enforced
 * in the service layer by checking {@code orderItem.order.status}.
 *
 * <p>The unique constraint {@code (customer_id, product_id)} ensures a customer
 * can review each product only once, regardless of how many times they bought it.
 *
 * <p>Extends {@link BaseEntity} — permanent record, no soft delete.
 */
@Entity
@Table(name = "reviews",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_review_customer_product",
               columnNames = {"customer_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * The specific order item that proves this is a verified purchase.
     * Used to check order ownership and completion status.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    /** Star rating from 1 (worst) to 5 (best). */
    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private ReviewStatus status = ReviewStatus.PENDING;

    /** Internal moderation note — never exposed to the customer. */
    @Column(name = "admin_note", length = 500)
    private String adminNote;
}
