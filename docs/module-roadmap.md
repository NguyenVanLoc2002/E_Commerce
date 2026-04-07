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
- auth (login/register/refresh token)
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
- category
- brand
- product
- product_variant
- product_media

### Mục tiêu:
- admin có thể tạo sản phẩm
- hiển thị product list + detail

---

## 4. Phase 3 - Inventory

### Modules:
- warehouse
- inventory
- inventory_reservation
- stock_movement

### Mục tiêu:
- quản lý tồn kho theo variant
- hỗ trợ reserve stock

---

## 5. Phase 4 - Cart

### Modules:
- cart
- cart_item

### Mục tiêu:
- customer thêm sản phẩm vào giỏ
- validate tồn kho trước checkout

---

## 6. Phase 5 - Order (CORE FLOW)

### Modules:
- order
- order_item

### Mục tiêu:
- tạo order từ cart
- snapshot dữ liệu
- validate business

---

## 7. Phase 6 - Payment (Basic)

### Modules:
- payment
- payment_transaction

### Mục tiêu:
- hỗ trợ COD trước
- chuẩn bị structure cho payment gateway

---

## 8. Phase 7 - Promotion

### Modules:
- voucher
- promotion
- promotion_rule
- voucher_usage

---

## 9. Phase 8 - Fulfillment

### Modules:
- shipment
- invoice

---

## 10. Phase 9 - Engagement

### Modules:
- review
- notification

---

## 11. Phase 10 - Admin & Optimization

- audit log
- dashboard
- reporting
- caching (Redis)
- search optimization

---

## 12. Nguyên tắc khi implement module

Theo CLAUDE.md :contentReference[oaicite:0]{index=0}:

Khi tạo module phải có:
- controller
- service
- dto
- entity
- repository
- mapper
- validation

---

## 13. Checklist hoàn thành 1 module

- [ ] Entity + migration (Flyway)
- [ ] Repository
- [ ] Service logic
- [ ] Controller API
- [ ] DTO request/response
- [ ] Validation
- [ ] Error handling
- [ ] Swagger docs
- [ ] Unit test cơ bản