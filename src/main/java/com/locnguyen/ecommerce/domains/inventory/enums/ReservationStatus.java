package com.locnguyen.ecommerce.domains.inventory.enums;

/**
 * Reservation lifecycle:
 *   PENDING → RELEASED (on cancel or complete) / EXPIRED (auto-release)
 */
public enum ReservationStatus {
    PENDING,
    RELEASED,
    EXPIRED,
    CANCELLED
}
