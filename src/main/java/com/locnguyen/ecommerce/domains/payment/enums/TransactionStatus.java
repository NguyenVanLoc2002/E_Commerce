package com.locnguyen.ecommerce.domains.payment.enums;

/**
 * Transaction-level status — records the outcome of each payment attempt.
 */
public enum TransactionStatus {
    INITIATED,
    SUCCESS,
    FAILED,
    REFUNDED,
    CANCELLED
}
