package com.locnguyen.ecommerce.domains.user.enums;

/**
 * System role names — must match exactly the seed data in Flyway V2.
 *
 * <p>Role hierarchy (defined in SecurityConfig):
 * <pre>
 *   SUPER_ADMIN > ADMIN > STAFF > CUSTOMER
 * </pre>
 */
public enum RoleName {
    SUPER_ADMIN,
    ADMIN,
    STAFF,
    CUSTOMER
}
