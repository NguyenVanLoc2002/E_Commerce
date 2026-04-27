# API Common Conventions

This document defines the conventions shared by all REST endpoints in the
ecommerce backend. It is the source of truth for clients (admin web, customer
web, mobile) when integrating with the API.

> Source: `com.locnguyen.ecommerce.common.*` and `domains.*.controller.*`. Update
> this file when those packages change — never the other way round.

---

## 1. Base path

All API endpoints are mounted under a single, versioned prefix:

```
/api/v1
```

The constant lives in `AppConstants.API_V1` and is concatenated into every
controller's `@RequestMapping`. There is no separate host or sub-domain.

A breaking change MUST go to a new prefix (`/api/v2`, …). Backwards-compatible
additions stay on `/api/v1`.

---

## 2. Authentication

The API is **stateless JWT** — no sessions, no cookies.

### 2.1. Bearer token header

```
Authorization: Bearer <accessToken>
```

The token is the `accessToken` field returned by `POST /api/v1/auth/login` (and
`POST /api/v1/auth/register` for new customers, which auto-logs-in).

### 2.2. Token pair

`/auth/login`, `/auth/register`, and `/auth/refresh-token` all return a
`TokenResponse`:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

- `accessToken` — short-lived; pass on every authenticated request.
- `refreshToken` — long-lived; pass to `POST /auth/refresh-token` to mint a new
  pair without re-prompting for the password.
- `expiresIn` — seconds until the access token expires.

### 2.3. Logout / blacklist

`POST /api/v1/auth/logout` (requires Bearer token) adds the current access
token to a Redis blacklist for the remainder of its TTL. Subsequent requests
with the same token receive `401 TOKEN_BLACKLISTED`.

### 2.4. Endpoints that DO NOT require a token

The Spring Security filter chain (`SecurityConfig`) permits these without auth:

| Method | Path                                  |
|--------|---------------------------------------|
| POST   | `/api/v1/auth/register`               |
| POST   | `/api/v1/auth/login`                  |
| POST   | `/api/v1/auth/refresh-token`          |
| POST   | `/api/v1/payments/callback`           |
| GET    | `/api/v1/products/**`                 |
| GET    | `/api/v1/categories/**`               |
| GET    | `/api/v1/brands/**`                   |
| GET    | `/swagger-ui/**`, `/v3/api-docs/**`   |
| GET    | `/actuator/health`, `/actuator/info`  |

> The payment-callback endpoint is unauthenticated because the gateway calls
> it. It is protected at the application layer by signature/idempotency checks.

Everything else is `authenticated()` — a missing or invalid Bearer token
returns `401 UNAUTHORIZED`.

### 2.5. Roles

Four roles are defined; they form a strict hierarchy enforced by Spring
Security's `RoleHierarchy`:

```
SUPER_ADMIN  >  ADMIN  >  STAFF  >  CUSTOMER
```

A user with a higher role automatically passes any `hasRole` / `hasAnyRole`
check for a lower role. `@PreAuthorize` uses this hierarchy.

URL-level rule: `/api/v1/admin/**` requires `STAFF` or above. Per-endpoint
`@PreAuthorize` may further restrict (e.g. ADMIN-only destructive ops).

---

## 3. Standard success response

All successful responses are wrapped by `ApiResponse<T>`:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Request processed successfully",
  "data": { ... },
  "timestamp": "2026-04-27T08:00:00Z"
}
```

| Field        | Type    | Notes                                                   |
|--------------|---------|---------------------------------------------------------|
| `success`    | boolean | Always `true` for 2xx responses                         |
| `code`       | string  | Always `"SUCCESS"` for non-error responses              |
| `message`    | string  | Human-readable description                              |
| `data`       | any     | Endpoint-specific payload (object, array, or `null`)    |
| `timestamp`  | string  | ISO-8601 UTC timestamp from `Instant.now()`             |

For "no body" results (DELETE, mark-as-read, etc.) `data` is `null` and the
HTTP status is still `200 OK`.

For `POST` endpoints that create a resource, `data` carries the new resource
and HTTP status is `201 Created`.

---

## 4. Standard error response

All errors are wrapped by `ErrorResponse`:

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "errors": [
    { "field": "email", "message": "must not be blank" }
  ],
  "timestamp": "2026-04-27T08:00:00Z",
  "path": "/api/v1/auth/register"
}
```

| Field        | Type    | Notes                                                   |
|--------------|---------|---------------------------------------------------------|
| `success`    | boolean | Always `false`                                          |
| `code`       | string  | Stable machine-readable error code (see §6)             |
| `message`    | string  | Human-readable description                              |
| `errors`     | array   | Field-level validation errors; absent for non-422 cases |
| `timestamp`  | string  | ISO-8601 UTC timestamp                                  |
| `path`       | string  | Request URI that produced the error                     |

`errors[].field` is the offending DTO field name (or object name for
class-level constraints). `errors[].message` is the validation message from
the constraint annotation.

---

## 5. HTTP status codes

The handler in `GlobalExceptionHandler` maps exceptions to:

| Status | When                                                                    |
|--------|-------------------------------------------------------------------------|
| 200    | Successful GET / PATCH / mutating action that returns data              |
| 201    | Successful POST that creates a resource                                 |
| 400    | Malformed JSON, missing required query param, type-mismatch on a path/query var, payment callback rejected by signature checks |
| 401    | Missing / expired / blacklisted token, invalid credentials              |
| 403    | Authenticated but lacks the required role; account disabled             |
| 404    | Resource not found, no handler for the URL                              |
| 405    | Method not allowed on this URL                                          |
| 409    | Unique-constraint violation, idempotency conflict (e.g. payment already processed) |
| 415    | Wrong media type                                                        |
| 422    | Bean-validation failure (DTO `@Valid`), business-rule violation         |
| 500    | Unhandled exception (logged, never exposed)                             |

The `code` field in the error body is far more useful than the HTTP status for
client logic — see §6.

---

## 6. Error code catalogue

Codes come from `com.locnguyen.ecommerce.common.exception.ErrorCode`. The full
list:

### General

| Code                    | HTTP | Default message                          |
|-------------------------|------|------------------------------------------|
| `BAD_REQUEST`           | 400  | Bad request                              |
| `UNAUTHORIZED`          | 401  | Unauthorized                             |
| `FORBIDDEN`             | 403  | Access denied                            |
| `NOT_FOUND`             | 404  | Resource not found                       |
| `VALIDATION_ERROR`      | 422  | Validation failed                        |
| `CONFLICT`              | 409  | Resource conflict                        |
| `INTERNAL_SERVER_ERROR` | 500  | An internal error occurred. Please try again later. |

### Auth

| Code                       | HTTP | Default message                                          |
|----------------------------|------|----------------------------------------------------------|
| `INVALID_CREDENTIALS`      | 401  | Invalid email or password                                |
| `TOKEN_EXPIRED`            | 401  | Token has expired                                        |
| `TOKEN_INVALID`            | 401  | Token is invalid                                         |
| `REFRESH_TOKEN_INVALID`    | 401  | Refresh token is invalid or expired                      |
| `TOKEN_BLACKLISTED`        | 401  | Token has been invalidated — please log in again         |
| `ACCOUNT_DISABLED`         | 403  | Your account has been disabled                           |
| `ACCOUNT_ALREADY_EXISTS`   | 409  | An account with this email already exists                |

### User / Customer

| Code                   | HTTP | Default message                  |
|------------------------|------|----------------------------------|
| `USER_NOT_FOUND`       | 404  | User not found                   |
| `CUSTOMER_NOT_FOUND`   | 404  | Customer not found               |
| `EMAIL_ALREADY_EXISTS` | 409  | Email is already registered      |
| `PHONE_ALREADY_EXISTS` | 409  | Phone number is already registered |

### Address

| Code                | HTTP | Default message  |
|---------------------|------|------------------|
| `ADDRESS_NOT_FOUND` | 404  | Address not found |

### Catalog (Brand, Category, Product, Variant)

| Code                          | HTTP | Default message                       |
|-------------------------------|------|---------------------------------------|
| `CATEGORY_NOT_FOUND`          | 404  | Category not found                    |
| `BRAND_NOT_FOUND`             | 404  | Brand not found                       |
| `SLUG_ALREADY_EXISTS`         | 409  | Slug is already in use                |
| `PRODUCT_NOT_FOUND`           | 404  | Product not found                     |
| `PRODUCT_INACTIVE`            | 422  | Product is currently inactive         |
| `PRODUCT_VARIANT_NOT_FOUND`   | 404  | Product variant not found             |
| `PRODUCT_VARIANT_INACTIVE`    | 422  | Product variant is currently inactive |
| `SKU_ALREADY_EXISTS`          | 409  | SKU is already in use                 |

### Inventory

| Code                       | HTTP | Default message                            |
|----------------------------|------|--------------------------------------------|
| `INVENTORY_NOT_FOUND`      | 404  | Inventory record not found                 |
| `INVENTORY_NOT_ENOUGH`     | 422  | Insufficient inventory for this variant    |
| `VARIANT_OUT_OF_STOCK`     | 422  | This variant is out of stock               |
| `STOCK_RESERVATION_FAILED` | 422  | Failed to reserve stock — please try again |
| `WAREHOUSE_NOT_FOUND`      | 404  | Warehouse not found                        |

### Cart

| Code                          | HTTP | Default message                          |
|-------------------------------|------|------------------------------------------|
| `CART_NOT_FOUND`              | 404  | Cart not found                           |
| `CART_ITEM_NOT_FOUND`         | 404  | Cart item not found                      |
| `CART_ITEM_QUANTITY_INVALID`  | 422  | Cart item quantity must be greater than 0 |

### Order

| Code                    | HTTP | Default message                                |
|-------------------------|------|------------------------------------------------|
| `ORDER_NOT_FOUND`       | 404  | Order not found                                |
| `ORDER_STATUS_INVALID`  | 422  | Invalid order status transition                |
| `ORDER_CANNOT_CANCEL`   | 422  | Order cannot be cancelled at its current status |
| `ORDER_CANNOT_COMPLETE` | 422  | Order cannot be completed at its current status |
| `ORDER_EMPTY`           | 422  | Order must contain at least one item           |

### Payment

| Code                        | HTTP | Default message                  |
|-----------------------------|------|----------------------------------|
| `PAYMENT_NOT_FOUND`         | 404  | Payment not found                |
| `PAYMENT_FAILED`            | 422  | Payment processing failed        |
| `PAYMENT_ALREADY_PROCESSED` | 409  | Payment has already been processed |
| `PAYMENT_CALLBACK_INVALID`  | 400  | Invalid payment callback received |

### Promotion / Voucher

| Code                            | HTTP | Default message                                       |
|---------------------------------|------|-------------------------------------------------------|
| `VOUCHER_NOT_FOUND`             | 404  | Voucher not found                                     |
| `VOUCHER_INVALID`               | 422  | Voucher is invalid                                    |
| `VOUCHER_EXPIRED`               | 422  | Voucher has expired                                   |
| `VOUCHER_USAGE_LIMIT_EXCEEDED`  | 422  | Voucher usage limit has been reached                  |
| `VOUCHER_NOT_APPLICABLE`        | 422  | Voucher is not applicable to this order               |
| `VOUCHER_MIN_ORDER_NOT_MET`     | 422  | Order amount does not meet the minimum required for this voucher |
| `VOUCHER_CODE_ALREADY_EXISTS`   | 409  | Voucher code is already in use                        |
| `VOUCHER_USER_LIMIT_EXCEEDED`   | 422  | You have reached the per-user usage limit for this voucher |
| `PROMOTION_NOT_FOUND`           | 404  | Promotion not found                                   |
| `PROMOTION_RULE_NOT_FOUND`      | 404  | Promotion rule not found                              |

### Shipment / Invoice

| Code                       | HTTP | Default message                            |
|----------------------------|------|--------------------------------------------|
| `SHIPMENT_NOT_FOUND`       | 404  | Shipment not found                         |
| `SHIPMENT_ALREADY_EXISTS`  | 409  | A shipment already exists for this order   |
| `SHIPMENT_STATUS_INVALID`  | 422  | Invalid shipment status transition         |
| `INVOICE_NOT_FOUND`        | 404  | Invoice not found                          |
| `INVOICE_ALREADY_EXISTS`   | 409  | An invoice already exists for this order   |
| `INVOICE_STATUS_INVALID`   | 422  | Invalid invoice status or transition       |

### Review

| Code                       | HTTP | Default message                                       |
|----------------------------|------|-------------------------------------------------------|
| `REVIEW_NOT_FOUND`         | 404  | Review not found                                      |
| `REVIEW_NOT_ELIGIBLE`      | 422  | You can only review products from completed orders    |
| `REVIEW_ALREADY_EXISTS`    | 409  | You have already reviewed this product                |
| `REVIEW_ALREADY_MODERATED` | 409  | Review has already been moderated                     |

### Notification

| Code                     | HTTP | Default message    |
|--------------------------|------|--------------------|
| `NOTIFICATION_NOT_FOUND` | 404  | Notification not found |

---

## 7. Pagination

List endpoints that return many items wrap the result in `PagedResponse<T>`,
which itself sits inside `ApiResponse<T>`:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Request processed successfully",
  "data": {
    "items": [ ... ],
    "page": 0,
    "size": 20,
    "totalItems": 125,
    "totalPages": 7,
    "hasNext": true,
    "hasPrevious": false
  },
  "timestamp": "2026-04-27T08:00:00Z"
}
```

### 7.1. Query parameters

The standard Spring Data `Pageable` binding is used:

| Param   | Type   | Default        | Notes                                    |
|---------|--------|----------------|------------------------------------------|
| `page`  | int    | `0`            | Zero-based page index                    |
| `size`  | int    | `20`           | `AppConstants.DEFAULT_PAGE_SIZE = 20`    |
| `sort`  | string | endpoint-specific | `field` or `field,direction` (e.g. `createdAt,desc`) |

Clients MAY pass `sort` multiple times to sort by multiple fields:
`?sort=createdAt,desc&sort=id,asc`.

### 7.2. Per-endpoint sort defaults

Most list endpoints set a sensible default with `@PageableDefault`. Highlights:

| Endpoint                                   | Default sort                       |
|--------------------------------------------|------------------------------------|
| `GET /api/v1/admin/products`               | `createdAt,desc`                   |
| `GET /api/v1/admin/brands`                 | `sortOrder,asc`                    |
| `GET /api/v1/admin/inventories`            | `updatedAt,desc`                   |
| `GET /api/v1/admin/invoices`               | `issuedAt,desc`                    |
| `GET /api/v1/admin/audit-logs`             | `createdAt,desc`                   |
| `GET /api/v1/admin/promotions`             | `createdAt,asc`                    |
| `GET /api/v1/admin/shipments`              | `createdAt,asc`                    |
| All others                                 | `createdAt` (direction varies)     |

### 7.3. Cap

`AppConstants.MAX_PAGE_SIZE = 100`. Spring will not enforce this automatically;
clients should not request larger pages.

---

## 8. Filtering

List endpoints take filter parameters via a dedicated `*Filter` DTO bound from
the query string (`@ModelAttribute`/implicit binding). Examples:

- `BrandFilter`: `name`, `status`
- `ProductFilter`: `keyword`, `categoryId`, `brandId`, `status`, `minPrice`, `maxPrice`, `featured`
- `OrderFilter`: `status`
- `OrderAdminFilter`: `customerId`, `status`, `paymentStatus`
- `PaymentFilter`: `method`, `status`, `orderCode`, `dateFrom`, `dateTo`
- `ShipmentFilter`: `orderId`, `orderCode`, `carrier`, `status`, `dateFrom`, `dateTo`
- `InvoiceFilter`: `invoiceCode`, `orderCode`, `status`, `dateFrom`, `dateTo`
- `InventoryFilter`: `variantId`, `warehouseId`, `productId`, `sku`, `keyword`, `variantStatus`, `outOfStock`, `lowStock`, `lowStockThreshold`
- `StockFilter`: `variantId`, `warehouseId`, `movementType`
- `ReviewFilter`: `status`, `productId`, `customerId`, `minRating`, `maxRating`
- `VoucherFilter`: `code`, `promotionId`, `active`, `dateFrom`, `dateTo`
- `PromotionFilter`: `name`, `scope`, `active`, `dateFrom`, `dateTo`
- `AuditLogFilter`: `entityType`, `entityId`, `action`, `actor`, `fromDate`, `toDate`

All filter fields are optional. Omitting a field removes it from the WHERE
clause.

### 8.1. Enum filters and case-insensitivity

Filter DTOs typed as enums (e.g. `OrderStatus`, `PaymentMethod`) accept the
enum **name** in any case. A global converter
(`WebMvcConfig.addFormatters`) trims and uppercases the input:

- `?status=PENDING` ✅
- `?status=pending` ✅
- `?status=Pending` ✅

JSON request bodies likewise accept any case via
`spring.jackson.mapper.accept-case-insensitive-enums=true`.

Unknown enum values produce `400 BAD_REQUEST`.

### 8.2. Date filters

Date filters use ISO-8601 (`yyyy-MM-dd`). `dateFrom` / `fromDate` is inclusive
at start-of-day; `dateTo` / `toDate` is inclusive at end-of-day (23:59:59).

---

## 9. Validation

Request bodies are validated by Jakarta Bean Validation:

- `@NotNull`, `@NotBlank` — required fields
- `@Size(min, max)` — string length / collection size
- `@Email` — email format
- `@Min`, `@Max` — numeric bounds
- `@Positive`, `@PositiveOrZero` — sign
- `@DecimalMin`, `@DecimalMax` — decimal bounds (with default-as-string)
- `@Pattern(regexp = ...)` — regex (used for password complexity, warehouse codes)

Failures produce `422 VALIDATION_ERROR` with an `errors[]` array listing every
violated field. Class-level `@Constraint` violations are listed under the
object name.

---

## 10. Versioning, observability, miscellaneous

### 10.1. Versioning

The current and only version is `/api/v1`. Breaking changes go to `/api/v2`.
Adding optional fields is non-breaking.

### 10.2. Request ID

Every request is tagged by `RequestLoggingFilter` with an `X-Request-ID`
header. If the client passes one in, the server reuses it; otherwise the
server generates a UUID. The response always echoes `X-Request-ID`. The same
ID is logged in MDC, surfaced in audit-log rows, and useful when correlating
logs with errors.

### 10.3. Idempotency

The following endpoints are idempotent on duplicate calls:

- `POST /api/v1/payments/order/{orderId}/initiate` — returns the in-flight
  payment record without creating a duplicate.
- `POST /api/v1/payments/callback` — duplicate `providerTxnId` are ignored.
- `POST /api/v1/admin/payments/order/{orderId}/complete` — already-PAID is a
  no-op.
- `POST /api/v1/admin/invoices/order/{orderId}/generate` — returns the
  existing invoice if one exists.

### 10.4. CORS

The CORS allow-list is configured in `WebMvcConfig.addCorsMappings`. By
default it allows `GET, POST, PUT, PATCH, DELETE, OPTIONS` on all `/api/**`
endpoints with credentials enabled. Allowed origins come from
`AppProperties.cors.allowedOrigins`.

### 10.5. OpenAPI / Swagger

OpenAPI 3 is enabled via `springdoc-openapi`. Default routes:

- `GET /v3/api-docs` — JSON spec
- `GET /swagger-ui/index.html` — interactive UI

Both are public on `dev`. Sensitive metadata is restricted on `prod` per
`SecurityConfig`.
