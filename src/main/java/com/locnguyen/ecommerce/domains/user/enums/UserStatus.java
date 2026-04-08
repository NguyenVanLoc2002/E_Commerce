package com.locnguyen.ecommerce.domains.user.enums;

/**
 * User account status.
 *
 * <ul>
 *   <li>{@code ACTIVE} — can log in and use the system</li>
 *   <li>{@code INACTIVE} — account deactivated by user or admin; cannot log in</li>
 *   <li>{@code LOCKED} — locked due to suspicious activity or admin action; cannot log in</li>
 * </ul>
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    LOCKED
}
