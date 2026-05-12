# CLAUDE.md

## 1. Project Context
Backend for a fashion e-commerce platform serving Admin Web, Customer Web, Mobile App, and Backend API.

Architecture: **Modular Monolith**.

Goal: build a clean, secure, maintainable Spring Boot backend for an e-commerce MVP, with clear module boundaries and production-oriented code.

Core domains: auth, user, customer, address, category, brand, product, product variant, product attribute, inventory, cart, wishlist, order, payment, promotion, shipment, invoice, review, notification, admin.

## 2. Tech Stack
Backend: Java 17, Spring Boot 3.x, Spring Web, Spring Security, Spring Data JPA, Spring Validation, Spring Cache, OpenAPI/Swagger, Flyway, Maven, Lombok, MapStruct.

Infrastructure: MariaDB, Redis, Docker/Docker Compose, MinIO or S3-compatible storage when media storage is needed.

Rules:
- Do not add heavy dependencies without a clear production reason.
- Preserve existing project conventions unless a change is clearly justified.

## 3. Package Structure
Base package should match the actual project package:

```text
com.locnguyen.ecommerce
```

Recommended structure:

```text
src/main/java/com/locnguyen/ecommerce
├── common
│   ├── config
│   ├── constants
│   ├── exception
│   ├── response
│   ├── security
│   ├── validation
│   ├── auditing
│   └── utils
├── domains
│   ├── auth
│   ├── user
│   ├── customer
│   ├── address
│   ├── category
│   ├── brand
│   ├── product
│   ├── inventory
│   ├── cart
│   ├── wishlist
│   ├── order
│   ├── payment
│   ├── promotion
│   ├── shipment
│   ├── invoice
│   ├── review
│   └── notification
└── infrastructure
    ├── cache
    ├── storage
    ├── external
    └── messaging
```

Each domain module usually contains: controller, service, repository, entity, dto, mapper, specification/query, enum.

## 4. Coding Principles
Rules:
- Write production-oriented code, not temporary happy-path code.
- Prefer clear naming over short naming.
- Do not put business logic in controllers.
- Do not expose JPA entities directly in API responses.
- Use request DTOs and response DTOs.
- Use MapStruct or explicit mapper classes.
- Keep transaction boundaries in the service layer.
- Avoid unrelated refactors in the same task.
- Avoid hidden side effects across modules.

Layer responsibility:
- Controller: receive request, validate DTO, call service, return response.
- Service: business validation, orchestration, transaction boundary.
- Repository: data access only.
- Mapper: entity/DTO conversion only.
- Specification/query: filtering, searching, dynamic query logic.

Before finishing code changes, check compile errors, broken imports, validation, authorization, transaction boundary, N+1 query risk, pagination/sorting, sensitive logs, and response wrapper consistency.

## 5. API Standards
Base API prefix:

```text
/api/v1
```

Response wrapper must be consistent:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "...",
  "data": {},
  "timestamp": "..."
}
```

API rules:
- Admin APIs should be under `/api/v1/admin/...`.
- Customer-facing APIs should be under `/api/v1/...`.
- Breaking changes should use a new version, for example `/api/v2`.
- List APIs must support pagination and sorting.
- Prefer Spring `Pageable` with `@PageableDefault`.
- Filter DTOs may be bound from query parameters by Spring.
- Payment webhooks should use `/api/v1/webhooks/payment/{provider}`.
- Webhooks must validate signature/HMAC and be idempotent.

Preferred pagination style:

```java
@PageableDefault(
    size = AppConstants.DEFAULT_PAGE_SIZE,
    sort = "createdAt",
    direction = Sort.Direction.DESC
) Pageable pageable
```

## 6. Validation and Error Handling
Request DTOs should use Bean Validation: `@NotNull`, `@NotBlank`, `@Size`, `@Email`, `@Positive`, `@Min`, `@Max`.

Business rules must be validated in service layer, for example quantity must not exceed inventory, voucher must be active and not expired, sale price must not exceed base price, order status transition must be valid, and payment callback must not be processed twice.

Use global exception handling.

Common error categories: validation error, authentication error, authorization error, not found, conflict, business rule violation, internal server error.

Use stable business error codes such as `PRODUCT_NOT_FOUND`, `VARIANT_NOT_FOUND`, `INVENTORY_NOT_ENOUGH`, `VOUCHER_INVALID`, `ORDER_STATUS_INVALID`, `PAYMENT_FAILED`.

## 7. Database and Migration Rules
Primary database: MariaDB.

Schema rules:
- All schema changes must go through Flyway migration.
- Do not edit old migrations that may already be applied.
- Do not use `ddl-auto=create` or `ddl-auto=update` in production.
- Money columns must use `DECIMAL(18,2)`.
- Use foreign keys for core relational data.
- Do not use cascade delete carelessly.
- Add indexes for columns used in filtering, joining, sorting, and uniqueness checks.

Key strategy: internal PK may use `BIGINT` auto increment. Public/business codes should be separate, for example `ORD202604060001`.

Soft delete:
- Use soft delete for admin/config/catalog data where recovery or history matters.
- Default list APIs should exclude deleted records unless the API explicitly supports `isDeleted` filtering.
- Do not hard delete unless the domain explicitly requires it.
- Do not overuse soft delete for large log/event tables.

Concurrency:
- Use optimistic locking with `@Version` for inventory or high-concurrency aggregate data.
- Avoid overselling by validating and updating inventory inside a safe transaction boundary.

## 8. Core Business Rules
Product and variant:
- `Product` is the parent product.
- `ProductVariant` is the actual sellable unit.
- SKU, size, color, price, and stock should be managed at variant level when applicable.

Inventory:
- Track inventory by variant.
- Do not cache available stock as source of truth.
- Inventory updates should be auditable through stock movement or equivalent history.

Order:
- `OrderItem` must store snapshot data at order time: product name, variant name, SKU, unit price, quantity, discount amount, line total.
- Order shipping address should be stored as a snapshot, not only as a foreign key to address.

Cart:
- Cart item should not store final price permanently.
- Recalculate price from current product/variant data at checkout.

Payment and promotion:
- Use both `payments` and `payment_transactions`.
- Payment callbacks must be idempotent.
- Do not trust client-side payment status.
- Separate promotion campaign, voucher code, rule, and usage history.
- Voucher usage must be protected from double-spending.

## 9. Authentication and Security Rules
Auth target:
- Access token is returned in response body.
- Refresh token is stored only in HttpOnly cookie.
- Refresh sessions are stored server-side in Redis with TTL.
- Store refresh token hash, not raw refresh token.
- Logout revokes refresh session, clears cookie, and blacklists access token when applicable.
- Password change/reset should revoke all active refresh sessions for that user.

Security requirements:
- Use BCrypt or Argon2 for password hashing.
- Use short-lived access tokens.
- Use role-based authorization.
- Configure CORS explicitly per environment.
- Validate all external input.
- Never log passwords, raw JWT, refresh token, OTP, cookies, or payment secrets.
- Do not return stacktrace in production API responses.
- Use rate limiting for auth, OTP, and payment-sensitive endpoints where applicable.
- Uploads must validate file type, size, and storage path.

Cookie guidance: refresh token cookie should use `HttpOnly`, use `Secure` in HTTPS environments, and choose `SameSite` based on frontend/backend domain setup.

## 10. Redis and Caching Rules
Redis may be used for refresh sessions, access-token blacklist, OTP with TTL, rate limiting, short-lived cache, and temporary checkout/session data if needed.

Good cache candidates: categories, brands, product detail with short TTL, homepage/banner configuration.

Avoid caching cart as source of truth, order status as source of truth, and inventory available quantity as source of truth.

Cache rules:
- Every cache must have a clear key naming convention.
- Every cache must have TTL.
- Update/delete operations must evict related cache.
- Do not cache sensitive user data unless the security model is clear.

## 11. Logging and Audit
Log useful operational events: auth failures, logout/session revocation, order status changes, inventory changes, payment callbacks, admin critical actions, external integration failures.

Do not log passwords, raw JWT, refresh token, OTP, payment secrets, or full cookie headers.

Admin actions that should be auditable: create/update/delete product, update inventory, change order status, create/update voucher, refund payment, lock/unlock user.

Prefer logs with request ID / trace ID when available.

## 12. Testing Expectations
When implementing or changing important logic, add or update tests where practical.

Prioritize unit tests for service business rules, validators, promotion calculation, inventory validation, and order status transitions.

Prioritize integration tests for repository queries, auth flow, order creation flow, voucher application, and payment callback idempotency.

For every feature, consider success path, validation error, not found, unauthorized/forbidden, conflict/business rule violation, and pagination/filtering if list API is affected.

## 13. Feature Implementation Checklist
When implementing a feature:
1. Read existing code and related docs first.
2. Identify affected domain rules.
3. Define or update entity, DTO, mapper, repository, service, controller.
4. Add Flyway migration if schema changes are needed.
5. Add validation and business error handling.
6. Add authorization checks.
7. Add pagination/filtering/sorting for list APIs.
8. Avoid N+1 queries and inefficient loops.
9. Add or update tests where valuable.
10. Update API documentation/contract if endpoint behavior changes.
11. Run or explain the expected verification command.

Do not make unrelated refactors in the same change unless required.

## 14. Documentation Rules
Update documentation when changing API endpoint path/request/response/error code, auth/cookie/session behavior, database schema, business lifecycle, or frontend/backend contract.

Important docs may include `README.md`, `admin-api-contract.md`, `customer-api-contract.md`, `api-common.md`, and domain-specific lifecycle docs.

Keep `CLAUDE.md` concise. Put long detailed explanations into separate docs.

## 15. Git Rules
Main development branch: `dev`.

Branch naming:

```text
{type}/{task_id}_{title}
```

Commit format:

```text
{type}({module}): {title}

{description of the change}

{Fixes/Complete #issue_number}
```

## 16. AI Working Rules
When acting as an AI coding assistant:
- Inspect existing code before changing code.
- Follow current naming, package, response, and error conventions.
- Do not change architecture without explaining why.
- Do not add dependencies without justification.
- Do not edit old Flyway migrations that may already be applied.
- Do not expose entities directly in API responses.
- Do not skip security and authorization checks.
- Do not ignore transaction boundaries.
- Do not hide errors with broad `catch Exception` blocks.
- Do not create fake success responses.
- Do not leave TODOs for required production behavior.

Before finishing a task, summarize files changed, behavior changed, tests or verification performed, and risks/follow-up work.

## 17. Early Phase Scope
Prioritize MVP backend: auth, user/customer, address, category, brand, product, product variant, inventory, cart, order, admin basic APIs.

Next phase: payment gateway, shipment, invoice, voucher/promotion, review, wishlist, notification, CMS/banner, dashboard/report.

Avoid early overengineering: microservices, event sourcing, full CQRS, complex recommendation engine, multi-tenant architecture, complex multi-currency/tax engine.
