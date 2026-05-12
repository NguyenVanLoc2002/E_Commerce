package com.locnguyen.ecommerce.domains.idempotency.enums;

public enum IdempotencyActionType {
    CHECKOUT,
    PAYMENT_INITIATE,
    PAYMENT_CALLBACK,
    ORDER_CONFIRM,
    ORDER_CANCEL,
    ORDER_STATUS_UPDATE,
    SHIPMENT_CREATE
}
