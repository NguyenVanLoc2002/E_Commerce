package com.locnguyen.ecommerce.domains.review.enums;

/**
 * Moderation status of a customer review.
 *
 * <pre>
 * PENDING ──► APPROVED
 *    │
 *    └──────► REJECTED
 * </pre>
 *
 * <p>Only {@link #APPROVED} reviews are visible to the public.
 * {@link #PENDING} reviews are visible to their author and admins only.
 */
public enum ReviewStatus {
    /** Submitted, awaiting admin moderation. */
    PENDING,
    /** Moderated and published. Visible to all. */
    APPROVED,
    /** Rejected during moderation. Not publicly visible. */
    REJECTED
}
