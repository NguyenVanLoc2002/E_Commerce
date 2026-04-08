package com.locnguyen.ecommerce.domains.address.enums;

/**
 * Address usage type — determines where the address can be used during checkout.
 *
 * <ul>
 *   <li>{@code SHIPPING} — delivery address for orders</li>
 *   <li>{@code BILLING} — invoice / receipt address</li>
 *   <li>{@code BOTH} — can be used for both shipping and billing</li>
 * </ul>
 */
public enum AddressType {
    SHIPPING,
    BILLING,
    BOTH
}
