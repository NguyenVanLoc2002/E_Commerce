# Admin API Contract

Audience: admin web and internal back-office tooling integrating with the backend at `T:\Project\ecommerce-backend`.

Source of truth:
- Admin controllers under `src/main/java/com/locnguyen/ecommerce/domains/admin/controller`
- Admin-only controllers still living in domain packages:
  - `src/main/java/com/locnguyen/ecommerce/domains/notification/controller/AdminNotificationController.java`
  - `src/main/java/com/locnguyen/ecommerce/domains/review/controller/AdminReviewController.java`
- DTOs, services, and shared response/error types under `src/main/java/com/locnguyen/ecommerce`

This document only describes endpoints implemented in the current source code.

---

## Common conventions

Base path:
- `/api/v1`

Authentication:
- All endpoints here require `Authorization: Bearer <accessToken>`.
- URL rule: `/api/v1/admin/**` requires at least `STAFF`.
- Role hierarchy: `SUPER_ADMIN > ADMIN > STAFF > CUSTOMER`.
- Method-level restrictions are applied with `@PreAuthorize` where stricter rules are needed.

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
    { "field": "name", "message": "Name is required" }
  ],
  "timestamp": "2026-04-27T10:00:00Z",
  "path": "/api/v1/admin/brands"
}
```

Pagination:
- Paged endpoints return `ApiResponse<PagedResponse<T>>`.
- `PagedResponse` fields: `items`, `page`, `size`, `totalItems`, `totalPages`, `hasNext`, `hasPrevious`.
- Standard Spring pageable params are `page`, `size`, `sort`, except `GET /api/v1/admin/reviews`, which uses `page`, `size`, `sort`, and `direction`.

Enum parsing:
- Query-string enums are case-insensitive.
- JSON-body enums are case-insensitive (`spring.jackson.mapper.accept-case-insensitive-enums=true`).

Common auth errors:
- `401 UNAUTHORIZED`
- `403 FORBIDDEN`

---

## 1. Audit logs

Role:
- `ADMIN`, `SUPER_ADMIN`

### GET `/api/v1/admin/audit-logs`

- HTTP: `200 OK`
- Query params (`AuditLogFilter`):
  - `entityType`
  - `entityId`
  - `action`
  - `actor`
  - `fromDate`
  - `toDate`
  - `page`, `size`, `sort`
- Response: `ApiResponse<PagedResponse<AuditLogResponse>>`
- Default sort: `createdAt DESC`
- `AuditLogResponse` fields:
  - `id`, `action`, `entityType`, `entityId`, `actor`, `ipAddress`, `requestId`, `details`, `createdAt`
- Notes:
  - Unknown `action` filter values are ignored by the specification instead of returning `400`.

### GET `/api/v1/admin/audit-logs/{id}`

- HTTP: `200 OK`
- Response: `ApiResponse<AuditLogResponse>`
- Errors:
  - `404 NOT_FOUND` or `404` from the business service when missing

---

## 2. Brands

Role:
- Read/write: `STAFF`, `ADMIN`, `SUPER_ADMIN`

### GET `/api/v1/admin/brands`

- HTTP: `200 OK`
- Query params (`BrandFilter`):
  - `name`
  - `status`
  - `page`, `size`, `sort`
- Response: `ApiResponse<PagedResponse<BrandResponse>>`
- Default sort: `sortOrder ASC`
- `BrandResponse` fields:
  - `id`, `name`, `slug`, `logoUrl`, `description`, `sortOrder`, `status`, `createdAt`

### POST `/api/v1/admin/brands`

- HTTP: `201 Created`
- Body (`CreateBrandRequest`):
  - `name`: required, max 100
  - `slug`: required, max 255
  - `logoUrl`: optional
  - `description`: optional
- Response: `ApiResponse<BrandResponse>`
- Errors:
  - `409 SLUG_ALREADY_EXISTS`
  - `422 VALIDATION_ERROR`

### PATCH `/api/v1/admin/brands/{id}`

- HTTP: `200 OK`
- Body (`UpdateBrandRequest`, all fields optional):
  - `name`, `slug`, `logoUrl`, `description`, `status`
- Response: `ApiResponse<BrandResponse>`
- Errors:
  - `404 BRAND_NOT_FOUND`
  - `409 SLUG_ALREADY_EXISTS`
  - `422 VALIDATION_ERROR`

### DELETE `/api/v1/admin/brands/{id}`

- HTTP: `200 OK`
- Response: `ApiResponse<Void>` with `data = null`
- Behavior: soft-delete
- Errors:
  - `404 BRAND_NOT_FOUND`

---

## 3. Categories

Role:
- Write-only admin surface: `STAFF`, `ADMIN`, `SUPER_ADMIN`

There is no admin category list or admin category detail endpoint in the current codebase. Read access is public via `/api/v1/categories`.

### POST `/api/v1/admin/categories`

- HTTP: `201 Created`
- Body (`CreateCategoryRequest`):
  - `parentId`: optional
  - `name`: required, max 100
  - `slug`: required, max 255
  - `description`: optional
  - `imageUrl`: optional
  - `sortOrder`: optional
- Response: `ApiResponse<CategoryResponse>`
- `CategoryResponse` fields:
  - `id`, `parentId`, `name`, `slug`, `description`, `imageUrl`, `status`, `sortOrder`, `createdAt`
- Errors:
  - `404 CATEGORY_NOT_FOUND` if `parentId` does not exist
  - `409 SLUG_ALREADY_EXISTS`
  - `422 VALIDATION_ERROR`

### PATCH `/api/v1/admin/categories/{id}`

- HTTP: `200 OK`
- Body (`UpdateCategoryRequest`, all fields optional):
  - `parentId`, `name`, `slug`, `description`, `imageUrl`, `status`, `sortOrder`
- Response: `ApiResponse<CategoryResponse>`
- Errors:
  - `404 CATEGORY_NOT_FOUND`
  - `409 SLUG_ALREADY_EXISTS`
  - `400 BAD_REQUEST` if `parentId == id`
  - `422 VALIDATION_ERROR`

### DELETE `/api/v1/admin/categories/{id}`

- HTTP: `200 OK`
- Response: `ApiResponse<Void>` with `data = null`
- Behavior: soft-delete
- Errors:
  - `404 CATEGORY_NOT_FOUND`

---

## 4. Products and variants

Role:
- `ADMIN`, `SUPER_ADMIN`

### GET `/api/v1/admin/products`

- HTTP: `200 OK`
- Query params (`ProductFilter`):
  - `keyword`
  - `categoryId`
  - `brandId`
  - `status`
  - `minPrice`
  - `maxPrice`
  - `featured`
  - `page`, `size`, `sort`
- Response: `ApiResponse<PagedResponse<ProductListItemResponse>>`
- Sorting:
  - The controller declares a pageable default, but clients should send an explicit `sort` if they need predictable ordering.

### GET `/api/v1/admin/products/{id}`

- HTTP: `200 OK`
- Response: `ApiResponse<ProductDetailResponse>`
- Errors:
  - `404 PRODUCT_NOT_FOUND`

### POST `/api/v1/admin/products`

- HTTP: `200 OK`
- Body (`CreateProductRequest`):
  - `name`: required, max 255
  - `slug`: required, max 255
  - `shortDescription`: optional, max 500
  - `description`: optional
  - `brandId`: optional
  - `categoryIds`: optional list
  - `status`: optional, defaults to `DRAFT`
  - `featured`: optional, defaults to `false`
- Response: `ApiResponse<ProductDetailResponse>`
- Errors:
  - `409 SLUG_ALREADY_EXISTS`
  - `404 BRAND_NOT_FOUND`
  - `422 VALIDATION_ERROR`
- Notes:
  - Unknown `categoryIds` are silently ignored by the current service. They do not produce `404`.

### PATCH `/api/v1/admin/products/{id}`

- HTTP: `200 OK`
- Body (`UpdateProductRequest`, all fields optional):
  - `name`, `slug`, `shortDescription`, `description`, `brandId`, `categoryIds`, `status`, `featured`
- Response: `ApiResponse<ProductDetailResponse>`
- Errors:
  - `404 PRODUCT_NOT_FOUND`
  - `404 BRAND_NOT_FOUND`
  - `409 SLUG_ALREADY_EXISTS`
  - `422 VALIDATION_ERROR`
- Notes:
  - When `categoryIds` is present, it replaces the category set.
  - Unknown `categoryIds` are silently ignored.

### DELETE `/api/v1/admin/products/{id}`

- HTTP: `200 OK`
- Response: `ApiResponse<Void>` with `data = null`
- Behavior: soft-delete
- Errors:
  - `404 PRODUCT_NOT_FOUND`

### GET `/api/v1/admin/products/{productId}/variants`

- HTTP: `200 OK`
- Response: `ApiResponse<List<VariantResponse>>`
- Notes:
  - Returns an empty list when no variants are found.
  - The current service does not validate that `productId` exists before listing.

### POST `/api/v1/admin/products/{productId}/variants`

- HTTP: `200 OK`
- Body (`CreateVariantRequest`):
  - `sku`: required, max 100
  - `barcode`: optional, max 100
  - `variantName`: required, max 255
  - `basePrice`: required, positive
  - `salePrice`: optional, positive
  - `compareAtPrice`: optional, positive
  - `weightGram`: optional
  - `status`: optional, defaults to `ACTIVE`
  - `attributes`: optional list of `{ attributeName, value }`, both required and max 100 when present
- Response: `ApiResponse<VariantResponse>`
- Errors:
  - `404 PRODUCT_NOT_FOUND`
  - `409 SKU_ALREADY_EXISTS`
  - `422 VALIDATION_ERROR`

### PATCH `/api/v1/admin/products/{productId}/variants/{variantId}`

- HTTP: `200 OK`
- Body (`UpdateVariantRequest`, all fields optional):
  - `sku`, `barcode`, `variantName`, `basePrice`, `salePrice`, `compareAtPrice`, `weightGram`, `status`, `attributes`
- Response: `ApiResponse<VariantResponse>`
- Errors:
  - `404 PRODUCT_VARIANT_NOT_FOUND`
  - `409 SKU_ALREADY_EXISTS`
  - `422 VALIDATION_ERROR`

### DELETE `/api/v1/admin/products/{productId}/variants/{variantId}`

- HTTP: `200 OK`
- Response: `ApiResponse<Void>` with `data = null`
- Behavior: soft-delete
- Errors:
  - `404 PRODUCT_VARIANT_NOT_FOUND`

Shared product DTO fields:
- `ProductListItemResponse`: `id`, `name`, `slug`, `shortDescription`, `thumbnailUrl`, `minPrice`, `maxPrice`, `status`, `featured`, `brandName`, `categoryNames`, `createdAt`
- `ProductDetailResponse`: `id`, `name`, `slug`, `shortDescription`, `description`, `status`, `featured`, `brand`, `categories`, `variants`, `media`, `createdAt`, `updatedAt`
- `VariantResponse`: `id`, `sku`, `barcode`, `variantName`, `basePrice`, `salePrice`, `compareAtPrice`, `weightGram`, `status`, `attributes`
- `AttributeResponse`: `name`, `value`
- `MediaResponse`: `id`, `mediaUrl`, `mediaType`, `sortOrder`, `primary`, `variantId`

---

## 5. Inventory

Role:
- Read: `STAFF`, `ADMIN`, `SUPER_ADMIN`
- Mutations: `ADMIN`, `SUPER_ADMIN`

### GET `/api/v1/admin/inventories`

- HTTP: `200 OK`
- Query params (`InventoryFilter`):
  - `variantId`
  - `warehouseId`
  - `productId`
  - `sku`
  - `keyword`
  - `variantStatus`
  - `outOfStock`
  - `lowStock`
  - `lowStockThreshold`
  - `page`, `size`, `sort`
- Response: `ApiResponse<PagedResponse<InventoryResponse>>`
- Default sort: `updatedAt DESC`
- `InventoryResponse` fields:
  - `id`, `variantId`, `variantName`, `sku`, `warehouseId`, `warehouseName`, `onHand`, `reserved`, `available`, `updatedAt`
- Notes:
  - `variantStatus` is a string filter. Invalid values are ignored by the specification.
  - `lowStockThreshold` defaults to `5` when `lowStock=true` and the threshold is omitted.

### GET `/api/v1/admin/inventories/variant/{variantId}`

- HTTP: `200 OK`
- Response: `ApiResponse<List<InventoryResponse>>`
- Notes:
  - Returns an empty list when nothing matches.
  - The service does not validate that `variantId` exists first.

### GET `/api/v1/admin/inventories/warehouse/{warehouseId}`

- HTTP: `200 OK`
- Response: `ApiResponse<List<InventoryResponse>>`
- Errors:
  - `404 WAREHOUSE_NOT_FOUND`

### GET `/api/v1/admin/inventories/variant/{variantId}/warehouse/{warehouseId}`

- HTTP: `200 OK`
- Response: `ApiResponse<InventoryResponse>`
- Errors:
  - `404 INVENTORY_NOT_FOUND`

### GET `/api/v1/admin/inventories/movements`

- HTTP: `200 OK`
- Query params:
  - `variantId`
  - `warehouseId`
  - `movementType`
  - `page`, `size`, `sort`
- Response: `ApiResponse<PagedResponse<StockMovementResponse>>`
- Repository ordering: `createdAt DESC`
- `StockMovementResponse` fields:
  - `id`, `variantId`, `variantName`, `sku`, `warehouseId`, `warehouseName`, `movementType`, `quantity`, `referenceType`, `referenceId`, `note`, `beforeOnHand`, `beforeReserved`, `beforeAvailable`, `afterOnHand`, `afterReserved`, `afterAvailable`, `createdBy`, `createdAt`

### POST `/api/v1/admin/inventories/adjust`

- Role: `ADMIN`, `SUPER_ADMIN`
- HTTP: `200 OK`
- Body (`AdjustStockRequest`):
  - `variantId`: required
  - `warehouseId`: required
  - `quantity`: required, positive
  - `movementType`: required, `IMPORT | EXPORT | RESERVE | RELEASE | ADJUST | RETURN`
  - `note`: optional, max 500
- Response: `ApiResponse<StockMovementResponse>`
- Errors:
  - `404 PRODUCT_VARIANT_NOT_FOUND`
  - `404 WAREHOUSE_NOT_FOUND`
  - `404 INVENTORY_NOT_FOUND`
  - `422 INVENTORY_NOT_ENOUGH`
  - `422 STOCK_RESERVATION_FAILED`
  - `422 VALIDATION_ERROR`
- Notes:
  - `movementType=ADJUST` is accepted, but the DTO only allows positive `quantity`, so this endpoint cannot perform negative manual corrections through the request contract.
  - `movementType=RESERVE` or `RELEASE` changes reserved counts directly and does not create/remove `InventoryReservation` rows. Dedicated `/reserve` and `/release` endpoints also exist.

### POST `/api/v1/admin/inventories/reserve`

- Role: `ADMIN`, `SUPER_ADMIN`
- HTTP: `200 OK`
- Body (`ReserveStockRequest`):
  - `variantId`: required
  - `warehouseId`: required
  - `quantity`: required, positive
  - `referenceType`: optional string
  - `referenceId`: optional string
  - `expiresAt`: optional timestamp
- Response: `ApiResponse<InventoryReservation>`
- Stable scalar fields on `InventoryReservation`:
  - `id`, `referenceType`, `referenceId`, `quantity`, `status`, `expiresAt`, `createdAt`, `createdBy`, `updatedAt`, `updatedBy`
- Errors:
  - `404 PRODUCT_VARIANT_NOT_FOUND`
  - `404 WAREHOUSE_NOT_FOUND`
  - `404 INVENTORY_NOT_FOUND`
  - `422 INVENTORY_NOT_ENOUGH`
  - `422 STOCK_RESERVATION_FAILED`
  - `422 VALIDATION_ERROR`
- Notes:
  - The controller returns the JPA entity directly instead of a dedicated DTO. Nested `variant` and `warehouse` serialization should not be treated as a stable public contract.

### POST `/api/v1/admin/inventories/release`

- Role: `ADMIN`, `SUPER_ADMIN`
- HTTP: `200 OK`
- Query params:
  - `referenceType`: required
  - `referenceId`: required
- Response: `ApiResponse<Void>` with `data = null`
- Behavior:
  - Releasing a reference with no pending reservations is a no-op.

---

## 6. Warehouses

Role:
- Read: `STAFF`, `ADMIN`, `SUPER_ADMIN`
- Mutations: `ADMIN`, `SUPER_ADMIN`

### GET `/api/v1/admin/warehouses`

- HTTP: `200 OK`
- Response: `ApiResponse<List<WarehouseResponse>>`
- Behavior: active warehouses only, ordered by `createdAt ASC`
- `WarehouseResponse` fields:
  - `id`, `name`, `code`, `location`, `status`, `createdAt`

### GET `/api/v1/admin/warehouses/{id}`

- HTTP: `200 OK`
- Response: `ApiResponse<WarehouseResponse>`
- Errors:
  - `404 WAREHOUSE_NOT_FOUND`

### POST `/api/v1/admin/warehouses`

- Role: `ADMIN`, `SUPER_ADMIN`
- HTTP: `200 OK`
- Body (`CreateWarehouseRequest`):
  - `name`: required, max 100
  - `code`: required, max 50, pattern `^[A-Za-z0-9_-]+$`
  - `location`: optional, max 255
- Response: `ApiResponse<WarehouseResponse>`
- Errors:
  - `400 BAD_REQUEST` if warehouse code already exists
  - `422 VALIDATION_ERROR`

### PATCH `/api/v1/admin/warehouses/{id}`

- Role: `ADMIN`, `SUPER_ADMIN`
- HTTP: `200 OK`
- Body (`UpdateWarehouseRequest`, all fields optional):
  - `name`, `location`, `status`
- Response: `ApiResponse<WarehouseResponse>`
- Errors:
  - `404 WAREHOUSE_NOT_FOUND`
  - `422 VALIDATION_ERROR`

### DELETE `/api/v1/admin/warehouses/{id}`

- Role: `ADMIN`, `SUPER_ADMIN`
- HTTP: `200 OK`
- Response: `ApiResponse<Void>` with `data = null`
- Behavior: soft-delete
- Errors:
  - `404 WAREHOUSE_NOT_FOUND`

---

## 7. Orders

Role:
- Read and non-cancel transitions: `STAFF`, `ADMIN`, `SUPER_ADMIN`
- Cancel: `ADMIN`, `SUPER_ADMIN`

### GET `/api/v1/admin/orders`

- HTTP: `200 OK`
- Query params (`OrderAdminFilter`):
  - `customerId`
  - `status`
  - `paymentStatus`
  - `page`, `size`, `sort`
- Response: `ApiResponse<PagedResponse<AdminOrderListItemResponse>>`
- Repository ordering: `createdAt DESC`
- `AdminOrderListItemResponse` fields:
  - `id`, `orderCode`, `customerId`, `customerName`, `customerEmail`, `status`, `paymentMethod`, `paymentStatus`, `totalItems`, `totalAmount`, `createdAt`

### GET `/api/v1/admin/orders/{id}`

- HTTP: `200 OK`
- Response: `ApiResponse<OrderResponse>`
- Errors:
  - `404 ORDER_NOT_FOUND`

### GET `/api/v1/admin/orders/code/{orderCode}`

- HTTP: `200 OK`
- Response: `ApiResponse<OrderResponse>`
- Errors:
  - `404 ORDER_NOT_FOUND`

### POST `/api/v1/admin/orders/{id}/confirm`

- HTTP: `200 OK`
- Response: `ApiResponse<OrderResponse>`
- Behavior:
  - Allowed from `PENDING` or `AWAITING_PAYMENT`.
  - Online orders also require `paymentStatus = PAID`.
- Errors:
  - `404 ORDER_NOT_FOUND`
  - `422 ORDER_STATUS_INVALID`

### POST `/api/v1/admin/orders/{id}/process`

- HTTP: `200 OK`
- Response: `ApiResponse<OrderResponse>`
- Behavior:
  - `CONFIRMED -> PROCESSING`
- Errors:
  - `404 ORDER_NOT_FOUND`
  - `422 ORDER_STATUS_INVALID`

### POST `/api/v1/admin/orders/{id}/deliver`

- HTTP: `200 OK`
- Response: `ApiResponse<OrderResponse>`
- Behavior:
  - `SHIPPED -> DELIVERED`
- Errors:
  - `404 ORDER_NOT_FOUND`
  - `422 ORDER_STATUS_INVALID`

### POST `/api/v1/admin/orders/{id}/complete`

- HTTP: `200 OK`
- Response: `ApiResponse<OrderResponse>`
- Behavior:
  - `DELIVERED -> COMPLETED`
  - Commits reserved stock via inventory completion
- Errors:
  - `404 ORDER_NOT_FOUND`
  - `422 ORDER_CANNOT_COMPLETE`

### POST `/api/v1/admin/orders/{id}/cancel`

- Role: `ADMIN`, `SUPER_ADMIN`
- HTTP: `200 OK`
- Response: `ApiResponse<OrderResponse>`
- Behavior:
  - Cancellable from `PENDING`, `AWAITING_PAYMENT`, or `CONFIRMED`
  - Releases reserved stock
- Errors:
  - `404 ORDER_NOT_FOUND`
  - `422 ORDER_CANNOT_CANCEL`

Shared order DTO fields:
- `OrderResponse`: `id`, `orderCode`, `customerId`, `status`, `paymentMethod`, `paymentStatus`, shipping snapshot fields, `subTotal`, `discountAmount`, `shippingFee`, `totalAmount`, `voucherCode`, `customerNote`, `items`, `createdAt`
- `OrderItemResponse`: `id`, `variantId`, `productName`, `variantName`, `sku`, `unitPrice`, `salePrice`, `quantity`, `lineTotal`

---

## 8. Payments

Role:
- `STAFF`, `ADMIN`, `SUPER_ADMIN`

### GET `/api/v1/admin/payments`

- HTTP: `200 OK`
- Query params (`PaymentFilter`):
  - `method`
  - `status`
  - `orderCode`
  - `dateFrom`
  - `dateTo`
  - `page`, `size`, `sort`
- Response: `ApiResponse<PagedResponse<PaymentResponse>>`
- Sorting:
  - No controller-level default sort is declared. Send `sort` explicitly if ordering matters.
- Notes:
  - List responses omit `transactions`.

### GET `/api/v1/admin/payments/{id}`

- HTTP: `200 OK`
- Response: `ApiResponse<PaymentResponse>`
- Errors:
  - `404 PAYMENT_NOT_FOUND`

### GET `/api/v1/admin/payments/code/{code}`

- HTTP: `200 OK`
- Response: `ApiResponse<PaymentResponse>`
- Errors:
  - `404 PAYMENT_NOT_FOUND`

### GET `/api/v1/admin/payments/order/{orderId}`

- HTTP: `200 OK`
- Response: `ApiResponse<PaymentResponse>`
- Errors:
  - `404 PAYMENT_NOT_FOUND`

### POST `/api/v1/admin/payments/order/{orderId}/complete`

- HTTP: `200 OK`
- Response: `ApiResponse<PaymentResponse>`
- Behavior:
  - COD-only operational flow
  - Idempotent if already `PAID`
- Errors:
  - `404 PAYMENT_NOT_FOUND`
  - `409 PAYMENT_ALREADY_PROCESSED`

### GET `/api/v1/admin/payments/{id}/transactions`

- HTTP: `200 OK`
- Response: `ApiResponse<List<TransactionResponse>>`
- Errors:
  - `404 PAYMENT_NOT_FOUND`

Shared payment DTO fields:
- `PaymentResponse`: `id`, `orderId`, `orderCode`, `paymentCode`, `method`, `status`, `amount`, `paidAt`, `transactions`, `createdAt`
- `TransactionResponse`: `id`, `transactionCode`, `status`, `amount`, `method`, `provider`, `providerTxnId`, `referenceType`, `referenceId`, `note`, `createdAt`

---

## 9. Shipments

Role:
- `STAFF`, `ADMIN`, `SUPER_ADMIN`

### POST `/api/v1/admin/shipments`

- HTTP: `201 Created`
- Body (`CreateShipmentRequest`):
  - `orderId`: required
  - `carrier`: required, max 100
  - `trackingNumber`: optional, max 200
  - `estimatedDeliveryDate`: optional date
  - `shippingFee`: optional, decimal >= 0
  - `note`: optional, max 500
- Response: `ApiResponse<ShipmentResponse>`
- Behavior:
  - Order must be in `PROCESSING`.
  - Creates an initial `PENDING` shipment event.
  - Transitions the order to `SHIPPED`.
- Errors:
  - `404 ORDER_NOT_FOUND`
  - `409 SHIPMENT_ALREADY_EXISTS`
  - `422 ORDER_STATUS_INVALID`
  - `422 VALIDATION_ERROR`

### GET `/api/v1/admin/shipments/{id}`

- HTTP: `200 OK`
- Response: `ApiResponse<ShipmentResponse>`
- Errors:
  - `404 SHIPMENT_NOT_FOUND`

### GET `/api/v1/admin/shipments/order/{orderId}`

- HTTP: `200 OK`
- Response: `ApiResponse<ShipmentResponse>`
- Errors:
  - `404 SHIPMENT_NOT_FOUND`

### GET `/api/v1/admin/shipments`

- HTTP: `200 OK`
- Query params (`ShipmentFilter`):
  - `orderId`
  - `orderCode`
  - `carrier`
  - `status`
  - `dateFrom`
  - `dateTo`
  - `page`, `size`, `sort`
- Response: `ApiResponse<PagedResponse<ShipmentResponse>>`
- Default sort: `createdAt ASC`
- Notes:
  - List responses omit `events`.

### PATCH `/api/v1/admin/shipments/{id}`

- HTTP: `200 OK`
- Body (`UpdateShipmentRequest`, all fields optional):
  - `carrier`, `trackingNumber`, `estimatedDeliveryDate`, `shippingFee`, `note`
- Response: `ApiResponse<ShipmentResponse>`
- Errors:
  - `404 SHIPMENT_NOT_FOUND`
  - `422 VALIDATION_ERROR`

### PATCH `/api/v1/admin/shipments/{id}/status`

- HTTP: `200 OK`
- Body (`UpdateShipmentStatusRequest`):
  - `status`: required
  - `location`: optional, max 255
  - `description`: required, max 500
  - `eventTime`: optional timestamp, defaults to now
- Response: `ApiResponse<ShipmentResponse>`
- Behavior:
  - Valid transitions are defined by `ShipmentStatus.canTransitionTo`.
  - On `DELIVERED`, the order is also moved to `DELIVERED`.
- Errors:
  - `404 SHIPMENT_NOT_FOUND`
  - `422 SHIPMENT_STATUS_INVALID`
  - `422 VALIDATION_ERROR`

Shared shipment DTO fields:
- `ShipmentResponse`: `id`, `orderId`, `orderCode`, `shipmentCode`, `carrier`, `trackingNumber`, `status`, `estimatedDeliveryDate`, `deliveredAt`, `shippingFee`, `note`, `events`, `createdAt`, `updatedAt`
- `ShipmentEventResponse`: `id`, `status`, `location`, `description`, `eventTime`

---

## 10. Invoices

Role:
- `STAFF`, `ADMIN`, `SUPER_ADMIN`

### POST `/api/v1/admin/invoices/order/{orderId}/generate`

- HTTP: `201 Created`
- Body: none
- Response: `ApiResponse<InvoiceResponse>`
- Behavior:
  - Idempotent if an invoice already exists.
  - Order must be in one of: `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `COMPLETED`.
- Errors:
  - `404 ORDER_NOT_FOUND`
  - `422 INVOICE_STATUS_INVALID`

### GET `/api/v1/admin/invoices/{id}`

- HTTP: `200 OK`
- Response: `ApiResponse<InvoiceResponse>`
- Errors:
  - `404 INVOICE_NOT_FOUND`

### GET `/api/v1/admin/invoices/order/{orderId}`

- HTTP: `200 OK`
- Response: `ApiResponse<InvoiceResponse>`
- Errors:
  - `404 INVOICE_NOT_FOUND`

### GET `/api/v1/admin/invoices/code/{invoiceCode}`

- HTTP: `200 OK`
- Response: `ApiResponse<InvoiceResponse>`
- Errors:
  - `404 INVOICE_NOT_FOUND`

### GET `/api/v1/admin/invoices`

- HTTP: `200 OK`
- Query params (`InvoiceFilter`):
  - `invoiceCode`
  - `orderCode`
  - `status`
  - `dateFrom`
  - `dateTo`
  - `page`, `size`, `sort`
- Response: `ApiResponse<PagedResponse<InvoiceResponse>>`
- Default sort: `issuedAt DESC`
- Notes:
  - List responses omit `items` and many detail fields.

### PATCH `/api/v1/admin/invoices/{id}/status`

- HTTP: `200 OK`
- Body (`UpdateInvoiceStatusRequest`):
  - `status`: required, only `PAID` or `VOIDED` are accepted by the service
  - `notes`: optional, max 1000
- Response: `ApiResponse<InvoiceResponse>`
- Errors:
  - `404 INVOICE_NOT_FOUND`
  - `422 INVOICE_STATUS_INVALID`
  - `422 VALIDATION_ERROR`

Shared invoice DTO fields:
- `InvoiceResponse`: `id`, `invoiceCode`, `status`, `issuedAt`, `dueDate`, `notes`, `orderId`, `orderCode`, `paymentMethod`, `paymentStatus`, `paidAt`, customer snapshot fields, billing snapshot fields, `items`, `subTotal`, `discountAmount`, `shippingFee`, `totalAmount`, `voucherCode`, `createdAt`
- `InvoiceItemResponse`: `variantId`, `productName`, `variantName`, `sku`, `unitPrice`, `salePrice`, `effectivePrice`, `quantity`, `lineTotal`

---

## 11. Promotions

Role:
- `ADMIN`, `SUPER_ADMIN`

### GET `/api/v1/admin/promotions`

- HTTP: `200 OK`
- Query params (`PromotionFilter`):
  - `name`
  - `scope`
  - `active`
  - `dateFrom`
  - `dateTo`
  - `page`, `size`, `sort`
- Response: `ApiResponse<PagedResponse<PromotionResponse>>`
- Default sort: `createdAt ASC`
- Notes:
  - `scope` is a string filter. Invalid values are ignored by the specification.
  - List responses omit `rules`.

### GET `/api/v1/admin/promotions/{id}`

- HTTP: `200 OK`
- Response: `ApiResponse<PromotionResponse>`
- Errors:
  - `404 PROMOTION_NOT_FOUND`

### POST `/api/v1/admin/promotions`

- HTTP: `201 Created`
- Body (`CreatePromotionRequest`):
  - `name`: required, max 200
  - `description`: optional, max 2000
  - `discountType`: required
  - `discountValue`: required, decimal >= 0.01
  - `maxDiscountAmount`: optional, decimal >= 0.01
  - `minimumOrderAmount`: optional, decimal >= 0
  - `scope`: required
  - `startDate`: required
  - `endDate`: required, must be after `startDate`
  - `usageLimit`: optional, min 1
- Response: `ApiResponse<PromotionResponse>`
- Errors:
  - `400 BAD_REQUEST` if `endDate` is not after `startDate`
  - `422 VALIDATION_ERROR`

### PATCH `/api/v1/admin/promotions/{id}`

- HTTP: `200 OK`
- Body (`UpdatePromotionRequest`, all fields optional):
  - `name`, `description`, `discountValue`, `maxDiscountAmount`, `minimumOrderAmount`, `startDate`, `endDate`, `active`, `usageLimit`
- Response: `ApiResponse<PromotionResponse>`
- Errors:
  - `404 PROMOTION_NOT_FOUND`
  - `400 BAD_REQUEST` if the resulting date range is invalid
  - `422 VALIDATION_ERROR`

### DELETE `/api/v1/admin/promotions/{id}`

- HTTP: `204 No Content`
- Response body: treat as empty
- Behavior: soft-delete
- Errors:
  - `404 PROMOTION_NOT_FOUND`

### POST `/api/v1/admin/promotions/{id}/rules`

- HTTP: `201 Created`
- Body (`AddRuleRequest`):
  - `ruleType`: required
  - `ruleValue`: required, max 500
  - `description`: optional, max 255
- Response: `ApiResponse<PromotionResponse>`
- Errors:
  - `404 PROMOTION_NOT_FOUND`
  - `422 VALIDATION_ERROR`

### DELETE `/api/v1/admin/promotions/{id}/rules/{ruleId}`

- HTTP: `204 No Content`
- Response body: treat as empty
- Errors:
  - `404 PROMOTION_NOT_FOUND`
  - `404 PROMOTION_RULE_NOT_FOUND`

Shared promotion DTO fields:
- `PromotionResponse`: `id`, `name`, `description`, `discountType`, `discountValue`, `maxDiscountAmount`, `minimumOrderAmount`, `scope`, `startDate`, `endDate`, `active`, `usageLimit`, `usageCount`, `rules`, `createdAt`, `updatedAt`
- `PromotionRuleResponse`: `id`, `ruleType`, `ruleValue`, `description`

---

## 12. Vouchers

Role:
- Read: `STAFF`, `ADMIN`, `SUPER_ADMIN`
- Mutations: `ADMIN`, `SUPER_ADMIN`

### GET `/api/v1/admin/vouchers`

- HTTP: `200 OK`
- Query params (`VoucherFilter`):
  - `code`
  - `promotionId`
  - `active`
  - `dateFrom`
  - `dateTo`
  - `page`, `size`, `sort`
- Response: `ApiResponse<PagedResponse<VoucherResponse>>`
- Sorting:
  - No controller-level default sort is declared. Send `sort` explicitly if ordering matters.

### GET `/api/v1/admin/vouchers/{id}`

- HTTP: `200 OK`
- Response: `ApiResponse<VoucherResponse>`
- Errors:
  - `404 VOUCHER_NOT_FOUND`

### GET `/api/v1/admin/vouchers/code/{code}`

- HTTP: `200 OK`
- Response: `ApiResponse<VoucherResponse>`
- Errors:
  - `404 VOUCHER_NOT_FOUND`

### GET `/api/v1/admin/vouchers/{id}/usages`

- HTTP: `200 OK`
- Query params:
  - `page`, `size`, `sort`
- Response: `ApiResponse<PagedResponse<VoucherUsageResponse>>`
- Errors:
  - `404 VOUCHER_NOT_FOUND`

### POST `/api/v1/admin/vouchers`

- Role: `ADMIN`, `SUPER_ADMIN`
- HTTP: `200 OK`
- Body (`CreateVoucherRequest`):
  - `code`: optional, max 100, auto-generated if blank
  - `promotionId`: required
  - `usageLimit`: optional, min 1
  - `usageLimitPerUser`: optional, min 1
  - `startDate`: required
  - `endDate`: required, must be after `startDate`
- Response: `ApiResponse<VoucherResponse>`
- Errors:
  - `404 PROMOTION_NOT_FOUND`
  - `409 VOUCHER_CODE_ALREADY_EXISTS`
  - `400 BAD_REQUEST` if `endDate` is not after `startDate`
  - `422 VALIDATION_ERROR`

### PATCH `/api/v1/admin/vouchers/{id}`

- Role: `ADMIN`, `SUPER_ADMIN`
- HTTP: `200 OK`
- Body (`UpdateVoucherRequest`, all fields optional):
  - `usageLimit`, `usageLimitPerUser`, `startDate`, `endDate`, `active`
- Response: `ApiResponse<VoucherResponse>`
- Errors:
  - `404 VOUCHER_NOT_FOUND`
  - `400 BAD_REQUEST` if the resulting date range is invalid
  - `422 VALIDATION_ERROR`

### DELETE `/api/v1/admin/vouchers/{id}`

- Role: `ADMIN`, `SUPER_ADMIN`
- HTTP: `200 OK`
- Response: `ApiResponse<Void>` with `data = null`
- Behavior: soft-delete
- Errors:
  - `404 VOUCHER_NOT_FOUND`

Shared voucher DTO fields:
- `VoucherResponse`: `id`, `code`, `promotionId`, `promotionName`, `discountType`, `discountValue`, `maxDiscountAmount`, `minimumOrderAmount`, `usageLimit`, `usageCount`, `usageLimitPerUser`, `startDate`, `endDate`, `active`, `createdAt`
- `VoucherUsageResponse`: `id`, `voucherId`, `voucherCode`, `customerId`, `orderId`, `discountAmount`, `usedAt`

---

## 13. Reviews

Two admin review surfaces exist in the current codebase.

### 13.1 Legacy admin review routes under `/api/v1/reviews`

Role:
- `GET /pending`, `GET /{id}`, `PATCH /{id}/moderate`: `STAFF`, `ADMIN`, `SUPER_ADMIN`
- `DELETE /{id}`: `ADMIN`, `SUPER_ADMIN`

#### GET `/api/v1/reviews/pending`

- HTTP: `200 OK`
- Query params:
  - `status`
  - `productId`
  - `customerId`
  - `minRating`
  - `maxRating`
  - `page`, `size`, `sort`
- Response: `ApiResponse<PagedResponse<ReviewResponse>>`
- Behavior:
  - If `status` is omitted, the service forces `PENDING`.

#### GET `/api/v1/reviews/{id}`

- HTTP: `200 OK`
- Response: `ApiResponse<ReviewResponse>`
- Errors:
  - `404 REVIEW_NOT_FOUND`

#### PATCH `/api/v1/reviews/{id}/moderate`

- HTTP: `200 OK`
- Body (`ModerateReviewRequest`):
  - `action`: required, only `APPROVED` or `REJECTED` are accepted by the service
  - `adminNote`: optional, max 500
- Response: `ApiResponse<ReviewResponse>`
- Errors:
  - `404 REVIEW_NOT_FOUND`
  - `409 REVIEW_ALREADY_MODERATED`
  - `400 BAD_REQUEST` for any action other than `APPROVED` or `REJECTED`
  - `422 VALIDATION_ERROR`

#### DELETE `/api/v1/reviews/{id}`

- HTTP: `200 OK`
- Response: `ApiResponse<Void>` with `data = null`
- Behavior: soft-delete
- Errors:
  - `404 REVIEW_NOT_FOUND`

### 13.2 Current admin review routes under `/api/v1/admin/reviews`

Role:
- `STAFF`, `ADMIN`, `SUPER_ADMIN`

#### GET `/api/v1/admin/reviews`

- HTTP: `200 OK`
- Query params:
  - `status`
  - `productId`
  - `customerId`
  - `minRating`
  - `maxRating`
  - `page`
  - `size`
  - `sort`
  - `direction`
- Response: `ApiResponse<PagedResponse<ReviewResponse>>`
- Sorting:
  - Defaults are `page=0`, `size=20`, `sort=createdAt`, `direction=desc`

#### GET `/api/v1/admin/reviews/{id}`

- HTTP: `200 OK`
- Response: `ApiResponse<ReviewResponse>`
- Errors:
  - `404 REVIEW_NOT_FOUND`

#### PATCH `/api/v1/admin/reviews/{id}/status`

- HTTP: `200 OK`
- Body (`UpdateReviewStatusRequest`):
  - `status`: required, only `APPROVED` or `REJECTED` are accepted by the service
  - `adminNote`: optional, max 500
- Response: `ApiResponse<ReviewResponse>`
- Errors:
  - `404 REVIEW_NOT_FOUND`
  - `409 REVIEW_ALREADY_MODERATED`
  - `400 BAD_REQUEST` for unsupported target statuses
  - `422 VALIDATION_ERROR`

Shared review DTO fields:
- `ReviewResponse`: `id`, `customerId`, `customerName`, `productId`, `productName`, `variantId`, `variantName`, `sku`, `orderItemId`, `rating`, `comment`, `status`, `adminNote`, `moderatedAt`, `moderatedBy`, `createdAt`, `updatedAt`

---

## 14. Notifications

Role:
- `ADMIN`, `SUPER_ADMIN`

### POST `/api/v1/admin/notifications/broadcast`

- HTTP: `201 Created`
- Body (`BroadcastNotificationRequest`):
  - `type`: required
  - `title`: required, max 255
  - `message`: required, max 5000
  - `referenceType`: optional, max 50
  - `referenceId`: optional, max 100
  - `customerIds`: optional list; empty or null means all customers
- Response: `ApiResponse<Map<String, Integer>>`
  - Data shape: `{ "sent": <count> }`
- Behavior:
  - `referenceId` is accepted as a string, but the service parses it to `Long`. Non-numeric values become `null`.
- Errors:
  - `422 VALIDATION_ERROR`

---

## 15. Users

Role:
- `ADMIN` and, via hierarchy, `SUPER_ADMIN`

### POST `/api/v1/admin/users`

- HTTP: `201 Created`
- Body (`CreateUserRequest`):
  - `email`: required, valid email, max 255
  - `password`: required, 8-64 chars, must contain lowercase, uppercase, and digit
  - `firstName`: required, max 100
  - `lastName`: optional, max 100
  - `phoneNumber`: optional, `@PhoneNumber`
  - `roles`: required non-empty set of `RoleName`
- Response: `ApiResponse<UserResponse>`
- `UserResponse` fields:
  - `id`, `email`, `firstName`, `lastName`, `phoneNumber`, `status`, `roles`, `createdAt`
- Errors:
  - `409 EMAIL_ALREADY_EXISTS`
  - `409 PHONE_ALREADY_EXISTS`
  - `400 BAD_REQUEST` for unknown role names
  - `422 VALIDATION_ERROR`
- Notes:
  - Despite the controller description, the service currently accepts any seeded `RoleName` value, including `CUSTOMER`, as long as the role exists in the database.

---

## Admin implementation notes

- `POST /api/v1/admin/products`, `POST /api/v1/admin/products/{productId}/variants`, and `POST /api/v1/admin/vouchers` currently return `200 OK`, not `201 Created`.
- `DELETE /api/v1/admin/promotions/{id}` and `DELETE /api/v1/admin/promotions/{id}/rules/{ruleId}` are annotated as `204 No Content`; clients should treat the body as empty.
- Unknown product category IDs on admin product create/update are ignored rather than rejected.
- `ReserveStockRequest.referenceType` and `referenceId` are optional in the real DTO, even though business flows usually populate them.
- `POST /api/v1/admin/inventories/reserve` returns a JPA entity instead of a dedicated DTO.
- Both legacy `/api/v1/reviews/...` admin moderation routes and newer `/api/v1/admin/reviews/...` routes are live.
