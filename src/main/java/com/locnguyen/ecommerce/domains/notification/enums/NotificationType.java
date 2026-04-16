package com.locnguyen.ecommerce.domains.notification.enums;

/**
 * Categories of in-app notifications.
 *
 * <p>Used both to drive notification template selection and to allow
 * customers to filter or mute specific notification categories.
 */
public enum NotificationType {

    // ─── Order lifecycle ────────────────────────────────────────────────────
    ORDER_CONFIRMED,
    ORDER_PROCESSING,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_COMPLETED,
    ORDER_CANCELLED,

    // ─── Payment ────────────────────────────────────────────────────────────
    PAYMENT_CONFIRMED,
    PAYMENT_FAILED,

    // ─── Review moderation ───────────────────────────────────────────────────
    REVIEW_APPROVED,
    REVIEW_REJECTED,

    // ─── Promotions ─────────────────────────────────────────────────────────
    VOUCHER_ISSUED,

    // ─── General ────────────────────────────────────────────────────────────
    SYSTEM
}
