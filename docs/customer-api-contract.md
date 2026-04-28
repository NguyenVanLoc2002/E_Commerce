# Customer API Contract

Audience: customer web/mobile clients integrating with the backend at `T:\Project\ecommerce-backend`.

Source of truth:
- Customer/public controllers under `src/main/java/com/locnguyen/ecommerce/domains/*/controller`
- DTOs under `src/main/java/com/locnguyen/ecommerce/domains/*/dto`
- Security and shared response/error types under `src/main/java/com/locnguyen/ecommerce/common`

This document only describes endpoints that exist in the current source code.

---

## Common conventions

Base path:
- `/api/v1`

Authentication:
- Public endpoints:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/refresh-token`
  - `POST /api/v1/payments/callback`
  - `GET /api/v1/products/**`
  - `GET /api/v1/categories/**`
  - `GET /api/v1/brands/**`
- Protected endpoints use `Authorization: Bearer <accessToken>`.
- Role hierarchy is `SUPER_ADMIN > ADMIN > STAFF > CUSTOMER`.
- Security-layer failures return generic:
  - `401 UNAUTHORIZED`
  - `403 FORBIDDEN`

Success wrapper:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Request processed successfully",
  "data": {},
  "timestamp": "2026-04-27T10:00:00Z"
}
```

Error wrapper:

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "errors": [
    { "field": "email", "message": "Email is required" }
  ],
  "timestamp": "2026-04-27T10:00:00Z",
  "path": "/api/v1/auth/register"
}
```

Pagination:
- Paged endpoints return `ApiResponse<PagedResponse<T>>`.
- `PagedResponse` fields: `items`, `page`, `size`, `totalItems`, `totalPages`, `hasNext`, `hasPrevious`.
- Standard query params: `page`, `size`, `sort`.

Enum parsing:
- Query-string enums are case-insensitive.
- JSON-body enums are case-insensitive (`spring.jackson.mapper.accept-case-insensitive-enums=true`).

Phone number validation:
- Fields annotated with `@PhoneNumber` accept `0xxxxxxxxx` or `+84xxxxxxxxx`.

---

## 1. Authentication

### POST `/api/v1/auth/register`

- Auth: public
- HTTP: `201 Created`
- Body (`RegisterRequest`):
  - `email`: required, valid email, max 255
  - `password`: required, 8-64 chars, must contain at least one lowercase letter, one uppercase letter, and one digit
  - `firstName`: required, max 100
  - `lastName`: optional, max 100
  - `phoneNumber`: optional, `@PhoneNumber`
- Response: `ApiResponse<AuthResponse>`
  - `AuthResponse.user`: `id`, `email`, `firstName`, `lastName`, `phoneNumber`, `status`, `roles`, `createdAt`
  - `AuthResponse.tokens`: `accessToken`, `refreshToken`, `tokenType`, `expiresIn`
- Errors:
  - `409 EMAIL_ALREADY_EXISTS`
  - `409 PHONE_ALREADY_EXISTS`
  - `422 VALIDATION_ERROR`

### POST `/api/v1/auth/login`

- Auth: public
- HTTP: `200 OK`
- Body (`LoginRequest`):
  - `email`: required, valid email
  - `password`: required
- Response: `ApiResponse<AuthResponse>`
- Errors:
  - `401 INVALID_CREDENTIALS`
  - `403 ACCOUNT_DISABLED`
  - `422 VALIDATION_ERROR`

### POST `/api/v1/auth/refresh-token`

- Auth: public
- HTTP: `200 OK`
- Body (`RefreshTokenRequest`):
  - `refreshToken`: required
- Response: `ApiResponse<TokenResponse>`
- Errors:
  - `401 REFRESH_TOKEN_INVALID`
  - `401 TOKEN_INVALID` if an access token is sent instead of a refresh token
  - `404 USER_NOT_FOUND`
  - `403 ACCOUNT_DISABLED`

### POST `/api/v1/auth/logout`

- Auth: Bearer token
- HTTP: `200 OK`
- Body: none
- Response: `ApiResponse<Void>` with `data = null`
- Errors:
  - `401 UNAUTHORIZED` if the endpoint is hit without authentication
  - `401 TOKEN_INVALID` if the controller cannot extract a valid Bearer header

---

## 2. Current user

### GET `/api/v1/me`

- Auth: Bearer token
- HTTP: `200 OK`
- Response: `ApiResponse<UserProfileResponse>`
  - Fields: `id`, `email`, `firstName`, `lastName`, `phoneNumber`, `status`, `roles`, `customerId`, `gender`, `birthDate`, `avatarUrl`, `loyaltyPoints`, `createdAt`
- Errors:
  - `401 UNAUTHORIZED`
  - `404 USER_NOT_FOUND`

### PATCH `/api/v1/me`

- Auth: Bearer token
- HTTP: `200 OK`
- Body (`UpdateProfileRequest`, all fields optional):
  - `firstName`: max 100
  - `lastName`: max 100
  - `phoneNumber`: `@PhoneNumber`
  - `gender`: `MALE | FEMALE | OTHER`
  - `birthDate`: ISO date
- Response: `ApiResponse<UserProfileResponse>`
- Errors:
  - `401 UNAUTHORIZED`
  - `404 USER_NOT_FOUND`
  - `409 PHONE_ALREADY_EXISTS`
  - `422 VALIDATION_ERROR`

---

## 3. Addresses

All address endpoints are scoped to the authenticated customer's own addresses.

### GET `/api/v1/addresses`

- Auth: Bearer token
- HTTP: `200 OK`
- Response: `ApiResponse<List<AddressResponse>>`
- Ordering: default address first, then newest first
- `AddressResponse` fields:
  - `id`, `receiverName`, `phoneNumber`, `streetAddress`, `ward`, `district`, `city`, `postalCode`, `addressType`, `isDefault`, `label`, `fullAddress`, `createdAt`

### GET `/api/v1/addresses/{id}`

- Auth: Bearer token
- HTTP: `200 OK`
- Response: `ApiResponse<AddressResponse>`
- Errors:
  - `404 ADDRESS_NOT_FOUND`

### POST `/api/v1/addresses`

- Auth: Bearer token
- HTTP: `201 Created`
- Body (`CreateAddressRequest`):
  - `receiverName`: required, max 100
  - `phoneNumber`: required, `@PhoneNumber`
  - `streetAddress`: required, max 255
  - `ward`: required, max 100
  - `district`: required, max 100
  - `city`: required, max 100
  - `postalCode`: optional, max 20
  - `addressType`: required, `SHIPPING | BILLING | BOTH`
  - `isDefault`: optional boolean
  - `label`: optional, max 50
- Response: `ApiResponse<AddressResponse>`
- Notes:
  - If `isDefault = true`, existing default addresses are cleared first.
- Errors:
  - `401 UNAUTHORIZED`
  - `422 VALIDATION_ERROR`

### PATCH `/api/v1/addresses/{id}`

- Auth: Bearer token
- HTTP: `200 OK`
- Body (`UpdateAddressRequest`, all fields optional):
  - `receiverName`, `phoneNumber`, `streetAddress`, `ward`, `district`, `city`, `postalCode`, `addressType`, `isDefault`, `label`
- Response: `ApiResponse<AddressResponse>`
- Errors:
  - `404 ADDRESS_NOT_FOUND`
  - `422 VALIDATION_ERROR`

### DELETE `/api/v1/addresses/{id}`

- Auth: Bearer token
- HTTP: `200 OK`
- Response: `ApiResponse<Void>` with `data = null`
- Behavior: soft-delete
- Errors:
  - `404 ADDRESS_NOT_FOUND`

---

## 4. Catalog

### GET `/api/v1/categories`

- Auth: public
- HTTP: `200 OK`
- Response: `ApiResponse<List<CategoryResponse>>`
- Behavior: lists only `ACTIVE` categories, ordered by `sortOrder ASC`
- `CategoryResponse` fields:
  - `id`, `parentId`, `name`, `slug`, `description`, `imageUrl`, `status`, `sortOrder`, `createdAt`

### GET `/api/v1/categories/{id}`

- Auth: public
- HTTP: `200 OK`
- Response: `ApiResponse<CategoryResponse>`
- Errors:
  - `404 CATEGORY_NOT_FOUND`
- Note:
  - Unlike the list endpoint, this service does not enforce `ACTIVE` status. Any non-deleted category ID can be returned.

### GET `/api/v1/brands`

- Auth: public
- HTTP: `200 OK`
- Response: `ApiResponse<List<BrandResponse>>`
- Behavior: lists only `ACTIVE` brands, ordered by `sortOrder ASC`
- `BrandResponse` fields:
  - `id`, `name`, `slug`, `logoUrl`, `description`, `sortOrder`, `status`, `createdAt`

### GET `/api/v1/brands/{id}`

- Auth: public
- HTTP: `200 OK`
- Response: `ApiResponse<BrandResponse>`
- Errors:
  - `404 BRAND_NOT_FOUND`
- Note:
  - Unlike the list endpoint, this service does not enforce `ACTIVE` status. Any non-deleted brand ID can be returned.

### GET `/api/v1/products`

- Auth: public
- HTTP: `200 OK`
- Query params:
  - `keyword`
  - `categoryId`
  - `brandId`
  - `minPrice`
  - `maxPrice`
  - `featured`
  - `page`, `size`, `sort`
- Response: `ApiResponse<PagedResponse<ProductListItemResponse>>`
- `ProductListItemResponse` fields:
  - `id`, `name`, `slug`, `shortDescription`, `thumbnailUrl`, `minPrice`, `maxPrice`, `status`, `featured`, `brandName`, `categoryNames`, `createdAt`
- Behavior:
  - Public listing always forces `status = PUBLISHED`.
  - If a client sends `status`, it is ignored by the service.
- Sorting:
  - The controller declares a pageable default, but clients should send an explicit `sort` if they need predictable ordering.

### GET `/api/v1/products/{id}`

- Auth: public
- HTTP: `200 OK`
- Response: `ApiResponse<ProductDetailResponse>`
- `ProductDetailResponse` fields:
  - `id`, `name`, `slug`, `shortDescription`, `description`, `status`, `featured`, `brand`, `categories`, `variants`, `media`, `createdAt`, `updatedAt`
- Nested `VariantResponse` fields:
  - `id`, `sku`, `barcode`, `variantName`, `basePrice`, `salePrice`, `compareAtPrice`, `weightGram`, `status`, `attributes`
- Nested `MediaResponse` fields:
  - `id`, `mediaUrl`, `mediaType`, `sortOrder`, `primary`, `variantId`
- Errors:
  - `404 PRODUCT_NOT_FOUND`
  - `422 PRODUCT_INACTIVE` if the product exists but is not `PUBLISHED`

---

## 5. Cart

### GET `/api/v1/cart`

- Auth: Bearer token
- HTTP: `200 OK`
- Response: `ApiResponse<CartResponse>`
- Behavior:
  - Creates an empty active cart on first access if none exists.
- `CartResponse` fields:
  - `id`, `items`, `totalItems`, `subTotal`, `updatedAt`
- `CartItemResponse` fields:
  - `id`, `variantId`, `variantName`, `sku`, `productSlug`, `productName`, `unitPrice`, `salePrice`, `quantity`, `availableStock`, `lineTotal`, `createdAt`

### POST `/api/v1/cart/items`

- Auth: Bearer token
- HTTP: `200 OK`
- Body (`AddCartItemRequest`):
  - `variantId`: required
  - `quantity`: required, min 1
- Response: `ApiResponse<CartResponse>`
- Behavior:
  - If the variant already exists in the cart, quantities are added together.
- Errors:
  - `404 PRODUCT_VARIANT_NOT_FOUND`
  - `422 PRODUCT_VARIANT_INACTIVE`
  - `422 INVENTORY_NOT_ENOUGH`
  - `422 VALIDATION_ERROR`

### PATCH `/api/v1/cart/items/{itemId}`

- Auth: Bearer token
- HTTP: `200 OK`
- Body (`UpdateCartItemRequest`):
  - `quantity`: required, min 1
- Response: `ApiResponse<CartResponse>`
- Errors:
  - `404 CART_NOT_FOUND`
  - `404 CART_ITEM_NOT_FOUND`
  - `422 INVENTORY_NOT_ENOUGH`
  - `422 VALIDATION_ERROR`

### DELETE `/api/v1/cart/items/{itemId}`

- Auth: Bearer token
- HTTP: `200 OK`
- Response: `ApiResponse<CartResponse>`
- Errors:
  - `404 CART_NOT_FOUND`
  - `404 CART_ITEM_NOT_FOUND`

### DELETE `/api/v1/cart`

- Auth: Bearer token
- HTTP: `200 OK`
- Response: `ApiResponse<Void>` with `data = null`
- Errors:
  - `404 CART_NOT_FOUND`

---

## 6. Orders

### POST `/api/v1/orders`

- Auth: Bearer token
- HTTP: `201 Created`
- Body (`CreateOrderRequest`):
  - `shippingAddressId`: required
  - `paymentMethod`: optional, `COD | ONLINE`, defaults to `COD`
  - `customerNote`: optional, max 500
  - `voucherCode`: optional string
- Response: `ApiResponse<OrderResponse>`
- `OrderResponse` fields:
  - `id`, `orderCode`, `customerId`, `status`, `paymentMethod`, `paymentStatus`
  - `receiverName`, `receiverPhone`, `shippingStreet`, `shippingWard`, `shippingDistrict`, `shippingCity`, `shippingPostalCode`
  - `subTotal`, `discountAmount`, `shippingFee`, `totalAmount`
  - `voucherCode`, `customerNote`, `items`, `createdAt`
- `OrderItemResponse` fields:
  - `id`, `variantId`, `productName`, `variantName`, `sku`, `unitPrice`, `salePrice`, `quantity`, `lineTotal`
- Behavior:
  - `COD` orders start in `PENDING`.
  - `ONLINE` orders start in `AWAITING_PAYMENT`.
  - Stock is reserved during order creation.
  - A COD payment record is created immediately.
  - Voucher application is not wired into order creation yet. `voucherCode` is stored, but `discountAmount` is currently always `0` and no voucher validation happens here.
- Errors:
  - `404 CART_NOT_FOUND`
  - `422 ORDER_EMPTY`
  - `404 ADDRESS_NOT_FOUND`
  - `404 INVENTORY_NOT_FOUND`
  - `422 INVENTORY_NOT_ENOUGH`
  - `422 STOCK_RESERVATION_FAILED`
  - `422 VALIDATION_ERROR`

### GET `/api/v1/orders`

- Auth: Bearer token
- HTTP: `200 OK`
- Query params:
  - `status`
  - `page`, `size`, `sort`
- Response: `ApiResponse<PagedResponse<OrderListItemResponse>>`
- `OrderListItemResponse` fields:
  - `id`, `orderCode`, `status`, `paymentMethod`, `paymentStatus`, `totalItems`, `totalAmount`, `createdAt`
- Sorting:
  - Repository query orders by `createdAt DESC`.

### GET `/api/v1/orders/{id}`

- Auth: Bearer token
- HTTP: `200 OK`
- Response: `ApiResponse<OrderResponse>`
- Ownership:
  - Returns `404 ORDER_NOT_FOUND` for another customer's order.
- Errors:
  - `404 ORDER_NOT_FOUND`

### POST `/api/v1/orders/my/{id}/cancel`

- Auth: Bearer token
- HTTP: `200 OK`
- Body: none
- Response: `ApiResponse<OrderResponse>`
- Behavior:
  - Customer cancellation is allowed only from `PENDING` or `AWAITING_PAYMENT`.
  - Reserved stock is released on success.
- Errors:
  - `404 ORDER_NOT_FOUND`
  - `422 ORDER_CANNOT_CANCEL`

---

## 7. Payments

### GET `/api/v1/payments/order/{orderId}`

- Auth: Bearer token
- HTTP: `200 OK`
- Response: `ApiResponse<PaymentResponse>`
- Ownership:
  - Returns `404 ORDER_NOT_FOUND` for another customer's order.
- Errors:
  - `404 ORDER_NOT_FOUND`
  - `404 PAYMENT_NOT_FOUND`

### POST `/api/v1/payments/order/{orderId}/initiate`

- Auth: Bearer token
- HTTP: `201 Created`
- Body (`InitPaymentRequest`) is optional:
  - `provider`: optional string
  - `returnUrl`: optional string
- Response: `ApiResponse<PaymentResponse>`
- `PaymentResponse` fields:
  - `id`, `orderId`, `orderCode`, `paymentCode`, `method`, `status`, `amount`, `paidAt`, `transactions`, `createdAt`
- `TransactionResponse` fields:
  - `id`, `transactionCode`, `status`, `amount`, `method`, `provider`, `providerTxnId`, `referenceType`, `referenceId`, `note`, `createdAt`
- Behavior:
  - Only works for orders whose `paymentMethod` is `ONLINE`.
  - If there is already a `PENDING` or `INITIATED` payment, the existing record is returned.
  - If the payment is `FAILED`, the service retries by resetting it to `INITIATED`.
  - If the payment is `PAID`, `REFUNDED`, or `PARTIALLY_REFUNDED`, the service rejects the request.
  - `returnUrl` is accepted by the DTO but is not used by the current service implementation.
- Errors:
  - `404 ORDER_NOT_FOUND`
  - `400 BAD_REQUEST` if the order is not an `ONLINE` payment order
  - `409 PAYMENT_ALREADY_PROCESSED`
  - `422 VALIDATION_ERROR` if a non-empty body fails DTO validation

### POST `/api/v1/payments/callback`

- Auth: public
- Intended caller: payment gateway
- HTTP: `200 OK`
- Body (`PaymentCallbackRequest`):
  - `orderCode`: required
  - `status`: required string, service treats only `SUCCESS` as success and everything else as failure
  - `providerTxnId`: optional
  - `provider`: optional
  - `payload`: optional raw payload string
- Response: `ApiResponse<PaymentResponse>`
- Errors:
  - `400 PAYMENT_CALLBACK_INVALID` if `orderCode` does not resolve
  - `404 PAYMENT_NOT_FOUND`
  - `409 PAYMENT_ALREADY_PROCESSED` for refunded payments
  - `422 VALIDATION_ERROR`

---

## 8. Voucher validation

### POST `/api/v1/vouchers/{code}/validate`

- Auth: Bearer token
- HTTP: `200 OK`
- Body (`ValidateVoucherRequest`):
  - `orderAmount`: required, decimal >= 0.01
  - `productIds`: optional list
  - `categoryIds`: optional list
  - `brandIds`: optional list
- Response: `ApiResponse<ValidateVoucherResponse>`
  - Fields: `voucherCode`, `promotionName`, `discountType`, `discountValue`, `discountAmount`, `orderAmount`, `finalAmount`
- Behavior:
  - This is preview-only.
  - It does not record voucher usage.
- Errors:
  - `404 VOUCHER_NOT_FOUND`
  - `422 VOUCHER_INVALID`
  - `422 VOUCHER_EXPIRED`
  - `422 VOUCHER_USAGE_LIMIT_EXCEEDED`
  - `422 VOUCHER_USER_LIMIT_EXCEEDED`
  - `422 VOUCHER_MIN_ORDER_NOT_MET`
  - `422 VOUCHER_NOT_APPLICABLE`
  - `500 INTERNAL_SERVER_ERROR` if a stored promotion rule value is malformed
  - `422 VALIDATION_ERROR`

---

## 9. Shipments

### GET `/api/v1/shipments/order/{orderId}`

- Auth: Bearer token
- HTTP: `200 OK`
- Response: `ApiResponse<ShipmentResponse>`
- `ShipmentResponse` fields:
  - `id`, `orderId`, `orderCode`, `shipmentCode`, `carrier`, `trackingNumber`, `status`, `estimatedDeliveryDate`, `deliveredAt`, `shippingFee`, `note`, `events`, `createdAt`, `updatedAt`
- `ShipmentEventResponse` fields:
  - `id`, `status`, `location`, `description`, `eventTime`
- Ownership:
  - Returns `404 ORDER_NOT_FOUND` for another customer's order.
- Errors:
  - `404 ORDER_NOT_FOUND`
  - `404 SHIPMENT_NOT_FOUND`

---

## 10. Invoices

### GET `/api/v1/invoices/order/{orderId}`

- Auth: Bearer token
- HTTP: `200 OK`
- Response: `ApiResponse<InvoiceResponse>`
- `InvoiceResponse` fields:
  - `id`, `invoiceCode`, `status`, `issuedAt`, `dueDate`, `notes`
  - `orderId`, `orderCode`, `paymentMethod`, `paymentStatus`, `paidAt`
  - `customerName`, `customerEmail`, `customerPhone`
  - `billingStreet`, `billingWard`, `billingDistrict`, `billingCity`, `billingPostalCode`
  - `items`, `subTotal`, `discountAmount`, `shippingFee`, `totalAmount`, `voucherCode`, `createdAt`
- `InvoiceItemResponse` fields:
  - `variantId`, `productName`, `variantName`, `sku`, `unitPrice`, `salePrice`, `effectivePrice`, `quantity`, `lineTotal`
- Ownership:
  - Returns `404 ORDER_NOT_FOUND` for another customer's order.
- Errors:
  - `404 ORDER_NOT_FOUND`
  - `404 INVOICE_NOT_FOUND`

---

## 11. Reviews

### POST `/api/v1/reviews`

- Auth: Bearer token with `CUSTOMER` role
- HTTP: `200 OK`
- Body (`CreateReviewRequest`):
  - `orderItemId`: required
  - `rating`: required, 1-5
  - `comment`: optional, max 2000
- Response: `ApiResponse<ReviewResponse>`
- `ReviewResponse` fields:
  - `id`, `customerId`, `customerName`, `productId`, `productName`, `variantId`, `variantName`, `sku`, `orderItemId`, `rating`, `comment`, `status`, `adminNote`, `moderatedAt`, `moderatedBy`, `createdAt`, `updatedAt`
- Behavior:
  - New reviews are stored as `PENDING`.
  - The source order must be `COMPLETED`.
  - One review per order item.
- Errors:
  - `404 ORDER_NOT_FOUND` for ownership mismatch
  - `422 REVIEW_NOT_ELIGIBLE` if the order item does not exist or the order is not completed
  - `409 REVIEW_ALREADY_EXISTS`
  - `422 VALIDATION_ERROR`

### GET `/api/v1/reviews/my`

- Auth: Bearer token with `CUSTOMER` role
- HTTP: `200 OK`
- Query params:
  - `page`, `size`, `sort`
- Response: `ApiResponse<PagedResponse<ReviewResponse>>`
- Sorting:
  - Repository orders by `createdAt DESC`.

### GET `/api/v1/reviews/product/{productId}`

- Auth: public
- HTTP: `200 OK`
- Query params:
  - `minRating`
  - `maxRating`
  - `page`, `size`, `sort`
- Response: `ApiResponse<PagedResponse<ReviewResponse>>`
- Behavior:
  - The service always forces `status = APPROVED`.
  - The path parameter always controls `productId`.
  - Query-string `status`, `productId`, and `customerId` are not meaningful for this endpoint.
- Sorting:
  - No explicit default sort is declared in the controller/service; send `sort` explicitly if ordering matters.

---

## 12. Notifications

All notification endpoints require `CUSTOMER` role.

### GET `/api/v1/notifications`

- Auth: Bearer token with `CUSTOMER` role
- HTTP: `200 OK`
- Query params:
  - `page`, `size`, `sort`
- Response: `ApiResponse<PagedResponse<NotificationResponse>>`
- `NotificationResponse` fields:
  - `id`, `type`, `title`, `body`, `referenceId`, `referenceType`, `read`, `readAt`, `createdAt`
- Sorting:
  - Repository orders by `createdAt DESC`.

### GET `/api/v1/notifications/unread-count`

- Auth: Bearer token with `CUSTOMER` role
- HTTP: `200 OK`
- Response: `ApiResponse<UnreadCountResponse>`
- `UnreadCountResponse` fields:
  - `count`

### PATCH `/api/v1/notifications/{id}/read`

- Auth: Bearer token with `CUSTOMER` role
- HTTP: `200 OK`
- Response: `ApiResponse<NotificationResponse>`
- Behavior:
  - Idempotent if already read.
- Errors:
  - `404 NOTIFICATION_NOT_FOUND`

### PATCH `/api/v1/notifications/read-all`

- Auth: Bearer token with `CUSTOMER` role
- HTTP: `200 OK`
- Response: `ApiResponse<Void>` with `data = null`

---

## Customer-facing implementation notes

- `voucherCode` on `POST /orders` is currently persisted but not validated or applied to totals.
- `GET /products` always returns published products even though `ProductFilter` contains a `status` field.
- `GET /brands/{id}` and `GET /categories/{id}` do not enforce `ACTIVE` status.
- Security-layer auth failures use `UNAUTHORIZED` / `FORBIDDEN` rather than token-specific codes.
- `POST /api/v1/reviews` currently returns `200 OK`, not `201 Created`.
