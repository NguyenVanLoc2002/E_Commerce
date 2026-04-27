# Module Roadmap

Tài liệu mô tả roadmap triển khai các module backend theo thứ tự tối ưu cho MVP → Scale.

---

## 1. Nguyên tắc xây dựng roadmap

- Ưu tiên module có dependency thấp trước
- Ưu tiên unlock business flow sớm
- Mỗi phase phải chạy được end-to-end flow
- Tránh build dàn trải nhiều module cùng lúc

---

## 2. Phase 1 - Core Foundation (BẮT BUỘC)

### 2.1. Auth & Security
- auth (register / login / logout / refresh token / forgot password / reset password / email verification)
- role / permission
- JWT security
- basic RBAC

### 2.2. User & Customer
- user
- customer
- address

---

## 3. Phase 2 - Catalog

### Modules:
- category (hỗ trợ parent-child)
- brand
- product
- product_variant
- product_media
- product_attributes / product_attribute_values / variant_attribute_values

### Mục tiêu:
- admin có thể tạo sản phẩm với đầy đủ biến thể
- hiển thị product list + detail + variants trên customer web
- filter theo category, brand, price

---

## 4. Phase 3 - Inventory

### Modules:
- warehouse
- inventory (có optimistic locking)
- inventory_reservation
- stock_movement

### Mục tiêu:
- quản lý tồn kho theo variant
- hỗ trợ reserve stock khi checkout
- log mọi biến động kho

---

## 5. Phase 4 - Cart

### Modules:
- cart
- cart_item

### Mục tiêu:
- customer thêm sản phẩm vào giỏ
- validate tồn kho trước checkout
- giá lấy từ variant tại thời điểm checkout (không lưu giá trong cart_item)

---

## 6. Phase 5 - Order (CORE FLOW)

### Modules:
- order
- order_item

### Mục tiêu:
- tạo order từ cart
- snapshot đầy đủ (sản phẩm, giá, địa chỉ giao hàng)
- validate business (tồn kho, địa chỉ)
- state machine chuyển trạng thái đơn hàng
- auto-cancel order AWAITING_PAYMENT quá hạn

---

## 7. Phase 6 - Payment (Basic)

### Modules:
- payment
- payment_transaction
- webhook handler (payment callback)

### Mục tiêu:
- hỗ trợ COD trước
- chuẩn bị structure cho payment gateway (VNPay / MoMo)
- xử lý callback idempotent
- validate chữ ký webhook

---

## 8. Phase 7 - Promotion & Voucher

### Modules:
- voucher
- voucher_usage
- promotion
- promotion_rule

### Mục tiêu:
- customer nhập voucher khi checkout
- validate điều kiện: min order, thời hạn, limit
- ghi lịch sử sử dụng

---

## 9. Phase 8 - Fulfillment

### Modules:
- shipment
- invoice
- refund (trong payment domain)

### Mục tiêu:
- tạo shipment khi xử lý đơn
- xuất invoice
- xử lý return & refund flow

---

## 10. Phase 9 - Engagement & Discovery

### Modules:
- review (chỉ cho phép với đơn hàng COMPLETED)
- wishlist / wishlist_item
- collection / collection_products

### Mục tiêu:
- customer đánh giá sản phẩm
- lưu sản phẩm yêu thích
- admin curate collection sản phẩm cho homepage / campaign

---

## 11. Phase 10 - Notification

### Modules:
- notification (in-app)

### Kênh:
- in-app
- email (order confirmation, shipment update)
- push notification sau này

---

## 12. Phase 11 - Admin & Optimization

- audit log
- dashboard metrics (doanh thu, đơn hàng, tồn kho thấp)
- Redis caching (category, brand, product detail)
- rate limiting
- search optimization (keyword filter, full-text nếu cần)

---

## 13. Phase 12 - Future (Ngoài MVP)

- loyalty / điểm tích lũy
- recommendation engine
- CMS / banner management nâng cao
- multi-warehouse allocation
- analytics / reporting phức tạp
- multiple payment gateways

---

## 14. Thứ tự implement trong 1 module

Theo CLAUDE.md:

Khi tạo module phải có:
1. Migration (Flyway)
2. Entity
3. Repository
4. Service logic + business validation
5. DTO request/response
6. Mapper
7. Controller API
8. Validation annotation
9. Error handling
10. Swagger docs

---

## 15. Checklist hoàn thành 1 module

- [ ] Entity + migration (Flyway)
- [ ] Repository (+ Specification nếu có filter)
- [ ] Service logic
- [ ] Controller API
- [ ] DTO request/response (tách rõ)
- [ ] Validation (annotation + business)
- [ ] Error handling (custom error code)
- [ ] Swagger / OpenAPI annotation đầy đủ
- [ ] Unit test service logic
- [ ] Integration test API chính

---

## 16. Dependency map

```
auth ──────────────────────────────────────┐
user / customer / address                  │
    │                                      │
    ▼                                      ▼
category / brand / product / variant    security config
    │
    ▼
inventory (depends on variant)
    │
    ▼
cart (depends on variant + inventory)
    │
    ▼
order (depends on cart + inventory + address)
    │
    ├── payment (depends on order)
    ├── shipment (depends on order)
    ├── invoice (depends on order)
    └── refund (depends on payment)

voucher / promotion ──── applies at order creation

review ─────────────────────────────── depends on order COMPLETED
wishlist ───────────────────────────── depends on product
collection ─────────────────────────── depends on product
notification ──────────────────────── triggered by order / payment events
```
