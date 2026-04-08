package com.locnguyen.ecommerce.domains.product.enums;

/**
 * Product lifecycle status.
 *
 * <ul>
 *   <li>{@code DRAFT} — being prepared, not visible to customers</li>
 *   <li>{@code PUBLISHED} — live on storefront</li>
 *   <li>{@code ARCHIVED} — no longer sold, hidden from storefront</li>
 * </ul>
 */
public enum ProductStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
