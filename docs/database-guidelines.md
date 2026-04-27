# Database Guidelines

Tài liệu quy tắc thiết kế database cho hệ thống backend bán quần áo.

---

## 1. Mục tiêu

Mục tiêu của guideline này:

- thiết kế database nhất quán
- dễ maintain
- dễ migration
- an toàn cho dữ liệu giao dịch
- hỗ trợ tốt cho nghiệp vụ e-commerce thời trang

---

## 2. Database engine

### Database chính
- **MariaDB**

### Lý do chọn
- phù hợp hệ transactional
- hỗ trợ quan hệ dữ liệu mạnh
- dễ vận hành
- dễ backup/restore
- tương thích tốt với Spring Data JPA

### Database bổ sung
- **Redis** cho cache / token / dữ liệu tạm
- **MongoDB** chỉ xem xét khi thực sự cần dữ liệu phi cấu trúc hoặc log lớn

---

## 3. Naming convention

### 3.1. Tên bảng
- dùng `snake_case`
- dùng **số nhiều**

Ví dụ:
- `users`
- `products`
- `product_variants`
- `order_items`
- `payment_transactions`

### 3.2. Tên cột
- dùng `snake_case`

Ví dụ:
- `created_at`
- `updated_at`
- `deleted_at`
- `is_deleted`
- `product_id`

### 3.3. Tên bảng nối
Với many-to-many hoặc mapping table:
- `user_roles`
- `product_categories`
- `variant_attribute_values`
- `collection_products`

---

## 4. Primary key strategy

### 4.1. Khuyến nghị
Dùng:
- `BIGINT AUTO_INCREMENT` cho primary key nội bộ

### 4.2. Business code riêng
Ngoài `id`, các thực thể giao dịch nên có `code` riêng để hiển thị công khai:

Ví dụ:
- `order_code`
- `invoice_code`
- `shipment_code`
- `refund_code`

Ví dụ giá trị:
- `ORD202604060001`
- `INV202604060010`
- `REF202604060003`

### 4.3. Vì sao không chỉ dùng ID công khai
- khó đọc với người dùng
- khó support vận hành
- dễ lộ pattern dữ liệu nội bộ

---

## 5. Audit columns chuẩn

Hầu hết bảng nghiệp vụ nên có các cột:

- `id`
- `created_at`
- `created_by`
- `updated_at`
- `updated_by`

Nếu dùng soft delete thì thêm:
- `is_deleted`
- `deleted_at`
- `deleted_by`

### Gợi ý base entity
Các entity quản trị và cấu hình thường kế thừa base entity audit.

---

## 6. Soft delete guideline

### 6.1. Khi nào nên dùng
Áp dụng cho:
- users
- customers
- addresses
- categories
- brands
- products
- product_variants
- vouchers
- banners
- cms_pages

### 6.2. Khi nào không nên lạm dụng
Không nên áp dụng hoặc cần cân nhắc với:
- payment_transactions
- audit_logs
- stock_movements

Các bảng log lớn có thể:
- lưu immutable
- archival thay vì soft delete

### 6.3. Quy tắc query
Query mặc định phải bỏ qua record bị soft delete.

---

## 7. Foreign key guideline

### 7.1. Phải có foreign key cho dữ liệu cốt lõi
Ví dụ:
- `product_variants.product_id -> products.id`
- `inventories.variant_id -> product_variants.id`
- `cart_items.cart_id -> carts.id`
- `order_items.order_id -> orders.id`
- `order_items.variant_id -> product_variants.id`
- `payments.order_id -> orders.id`
- `refunds.payment_id -> payments.id`

### 7.2. Không cascade delete bừa bãi
Không khuyến nghị `ON DELETE CASCADE` trên dữ liệu giao dịch quan trọng.

Ví dụ không nên cascade:
- từ `orders` xuống `order_items`
- từ `products` xuống `order_items`

### 7.3. Khuyến nghị
- ưu tiên `RESTRICT`
- xóa mềm ở application layer
- xử lý dữ liệu phụ thuộc một cách có kiểm soát

---

## 8. Index guideline

### 8.1. Bắt buộc index
Index cho:
- primary key
- foreign key
- business code unique
- các field filter/search dùng thường xuyên

### 8.2. Ví dụ field nên index
- `users.email`
- `users.phone_number`
- `products.slug`
- `products.status`
- `brands.slug`
- `categories.slug`
- `product_variants.sku`
- `orders.order_code`
- `orders.customer_id`
- `orders.order_status`
- `payments.order_id`
- `vouchers.code`

### 8.3. Composite index gợi ý
- `(variant_id, warehouse_id)` trên `inventories`
- `(customer_id, created_at)` trên `orders`
- `(order_id, status)` trên `payment_transactions`
- `(product_id, status)` trên `reviews`
- `(category_id, status)` nếu query sản phẩm theo category thường xuyên
- `(voucher_id, customer_id)` trên `voucher_usages`

### 8.4. Không index quá nhiều
Quá nhiều index sẽ làm chậm:
- insert
- update
- delete
- migration

Chỉ index field thực sự cần.

---

## 9. Unique constraint guideline

### Nên unique:
- `users.email`
- `users.phone_number` nếu yêu cầu duy nhất
- `products.slug`
- `brands.slug`
- `categories.slug`
- `product_variants.sku`
- `orders.order_code`
- `shipments.shipment_code`
- `invoices.invoice_code`
- `refunds.refund_code`
- `vouchers.code`

### Unique theo cặp
Ví dụ:
- `inventories(variant_id, warehouse_id)`
- `user_roles(user_id, role_id)`
- `collection_products(collection_id, product_id)`
- `wishlist_items(wishlist_id, product_id)`

---

## 10. Data type guideline

### 10.1. Money
Tất cả giá trị tiền tệ dùng:
```sql
DECIMAL(18,2)
```

Áp dụng cho:

* base_price
* sale_price
* compare_at_price
* subtotal_amount
* discount_amount
* shipping_fee
* total_amount
* paid_amount
* refund_amount

Không dùng:

* FLOAT
* DOUBLE

### 10.2. Quantity
Dùng:

* INT cho quantity thông thường
* BIGINT nếu có quy mô cực lớn

### 10.3. Boolean
Dùng:

* BOOLEAN hoặc TINYINT(1) theo mapping MariaDB

### 10.4. Text

* VARCHAR(n) cho field ngắn
* TEXT cho mô tả dài
* LONGTEXT nếu thật sự cần

---

## 11. Enum strategy

### 11.1. Khuyến nghị
Lưu enum dưới dạng VARCHAR, không phụ thuộc DB enum native.
Ví dụ:

* order_status
* payment_status
* shipment_status
* voucher_type

### 11.2. Lợi ích

* dễ đọc dữ liệu trực tiếp
* dễ migration
* ít lock-in hơn
* dễ thêm trạng thái mới

### 11.3. Lưu ý
Phải kiểm soát enum ở code. Không để giá trị tùy tiện.

---

## 12. Timestamp strategy

### 12.1. Chuẩn cột thời gian

* created_at
* updated_at
* deleted_at
* paid_at
* shipped_at
* delivered_at
* completed_at
* cancelled_at
* returned_at
* refunded_at

### 12.2. Timezone
Khuyến nghị backend lưu timestamp theo UTC.

### 12.3. Default
Có thể dùng application set timestamp. Nếu DB set mặc định thì phải thống nhất rõ chiến lược.

---

## 13. Optimistic Locking

### 13.1. Khi nào cần
Các bảng có khả năng bị concurrent update cao phải dùng optimistic locking:
- `inventories`: tránh oversell

### 13.2. Cách implement
Thêm cột `version BIGINT NOT NULL DEFAULT 0` và dùng `@Version` của JPA.

```sql
ALTER TABLE inventories ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
```

JPA tự động xử lý `OptimisticLockException` khi có conflict. Service cần catch và retry hoặc trả lỗi phù hợp.

---

## 14. JSON column guideline

### 14.1. Khi nào được dùng
Chỉ dùng cho dữ liệu phụ, ít join, ít filter.
Ví dụ:

* callback payload của payment gateway
* metadata bổ sung
* extra_info không cố định

### 14.2. Không dùng JSON cho dữ liệu lõi
Không nên dùng JSON cho:

* order items
* product variants
* inventory
* voucher usage lõi

Những dữ liệu này phải model hóa quan hệ rõ ràng.

---

## 15. Table design guideline theo domain

### 15.1. Identity & Access
**users**
Chứa: email, phone number, password hash, status, last login, audit columns

**roles**
Ví dụ: SUPER_ADMIN, ADMIN, STAFF, CUSTOMER

**permissions**
Nếu hệ thống cần RBAC chi tiết.

**user_roles**
Mapping nhiều-nhiều giữa user và role.

**customers**
Thông tin nghiệp vụ customer có thể tách khỏi users.

**addresses**
Nhiều địa chỉ cho customer: shipping address, billing address. Thêm cờ `is_default`.

### 15.2. Catalog
**categories**
- parent_id, name, slug, status, sort_order

**brands**
- name, slug, logo_url, status

**products**
- name, slug, short_description, description, brand_id, status, publish_status, metadata fields (seo_title, seo_description, seo_keywords)

**product_variants**
- product_id, sku, barcode, variant_name, base_price, sale_price, compare_at_price, weight, status

**product_media**
- product_id, variant_id (nullable), media_url, media_type, sort_order, is_primary

**product_attributes**
- name, code, type (VARIANT_OPTION | DESCRIPTIVE)

**product_attribute_values**
- attribute_id, value, display_value, color_hex (nullable, nếu là màu)

**variant_attribute_values**
- variant_id, attribute_value_id

**collections**
- name, slug, description, image_url, status, sort_order, start_at (nullable), end_at (nullable)

**collection_products**
- collection_id, product_id, sort_order

### 15.3. Inventory
**warehouses**
- name, code, location, status

**inventories**
- variant_id, warehouse_id, on_hand, reserved, available, version

**inventory_reservations**
- variant_id, order_id, quantity, expires_at, status

**stock_movements**
- variant_id, warehouse_id, movement_type, quantity, reference_type, reference_id, reason, note, created_by, created_at

### 15.4. Commerce
**carts**
- customer_id, status

**cart_items**
- cart_id, variant_id, quantity
- Lưu ý: KHÔNG lưu giá trong cart_items, giá lấy từ variant tại thời điểm checkout

**wishlists**
- customer_id

**wishlist_items**
- wishlist_id, product_id, added_at

**orders**
- order_code, customer_id, order_status, payment_status, shipment_status
- subtotal_amount, discount_amount, shipping_fee, total_amount
- voucher_code, voucher_discount_amount (snapshot)
- note, source (MOBILE_APP, CUSTOMER_WEB, ADMIN_WEB)
- Snapshot địa chỉ giao hàng:
  - shipping_recipient_name
  - shipping_recipient_phone
  - shipping_address_line
  - shipping_ward
  - shipping_district
  - shipping_province
  - shipping_country (mặc định VN)

**order_items**
- order_id, product_id, variant_id
- product_name, variant_name, sku (snapshot)
- unit_price, quantity, discount_amount, line_total (snapshot)
- thumbnail_url (snapshot, optional)

**shipments**
- order_id, shipment_code, carrier, tracking_number, shipping_fee, shipment_status
- shipped_at, delivered_at, estimated_delivery_at
- note

**invoices**
- order_id, invoice_code, invoice_status, invoice_amount, tax_amount
- issued_at

### 15.5. Payment
**payments**
- order_id, payment_method, payment_provider, payment_status
- payable_amount, paid_amount
- external_reference (provider's transaction ID)
- paid_at

**payment_transactions**
- payment_id, transaction_code, provider_transaction_id
- transaction_status, amount
- request_payload (JSON), response_payload (JSON)
- created_at

**refunds**
- payment_id, order_id, refund_code
- refund_amount, refund_status
- refund_reason, refund_note
- processed_by, processed_at, created_at

### 15.6. Promotion
**promotions**
- name, code, description, promotion_type, status, start_at, end_at, priority

**promotion_rules**
- promotion_id, rule_type, condition_json, discount_type, discount_value, max_discount_amount

**vouchers**
- code, voucher_type, discount_type, discount_value
- min_order_value, max_discount_amount
- usage_limit, per_user_limit, used_count
- applicable_to (ALL, CATEGORY, PRODUCT)
- start_at, end_at, status

**voucher_usages**
- voucher_id, customer_id, order_id, discount_amount, used_at

### 15.7. Engagement
**reviews**
- product_id, variant_id (nullable), customer_id, order_id
- rating (1-5), comment, status (PENDING, APPROVED, REJECTED)
- is_verified_purchase

**notifications**
- customer_id (nullable, null = broadcast), type, title, content
- is_read, read_at, created_at

**banners**
- title, image_url, redirect_url, sort_order, status, position
- start_at (nullable), end_at (nullable)

**audit_logs**
- actor_id, actor_type (USER, SYSTEM), action, entity_type, entity_id
- old_value (JSON, nullable), new_value (JSON, nullable)
- ip_address, created_at

---

## 16. Migration convention

### 16.1. Dùng Flyway
Mọi thay đổi schema phải qua migration file.

### 16.2. Tên file
```
V1__init_schema.sql
V2__create_users_roles_tables.sql
V3__create_catalog_tables.sql
V4__create_inventory_tables.sql
V5__create_order_tables.sql
V6__create_payment_tables.sql
V7__create_promotion_tables.sql
V8__create_engagement_tables.sql
```

### 16.3. Quy tắc
- 1 migration = 1 mục tiêu rõ ràng
- không sửa migration cũ đã chạy ở shared env
- muốn thay đổi thì tạo migration mới
- migration phải idempotent trong phạm vi Flyway quản lý

---

## 17. Sample index recommendations

**products**
- unique index slug
- index brand_id
- index status
- index created_at

**product_variants**
- unique index sku
- index product_id
- index status

**inventories**
- unique index (variant_id, warehouse_id)

**orders**
- unique index order_code
- index customer_id
- index order_status
- index created_at

**order_items**
- index order_id
- index product_id
- index variant_id

**payments**
- unique hoặc index order_id
- index payment_status

**payment_transactions**
- unique index transaction_code
- index payment_id
- index provider_transaction_id

**vouchers**
- unique index code
- index status
- index start_at
- index end_at

**voucher_usages**
- index (voucher_id, customer_id)
- index order_id

---

## 18. DDL ví dụ

### 18.1. products
```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    short_description VARCHAR(500) NULL,
    description TEXT NULL,
    brand_id BIGINT NULL,
    status VARCHAR(50) NOT NULL,
    publish_status VARCHAR(50) NOT NULL,
    seo_title VARCHAR(255) NULL,
    seo_description VARCHAR(500) NULL,
    seo_keywords VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    created_by VARCHAR(100) NULL,
    updated_at DATETIME NOT NULL,
    updated_by VARCHAR(100) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME NULL,
    deleted_by VARCHAR(100) NULL,
    CONSTRAINT uq_products_slug UNIQUE (slug)
);
```

### 18.2. product_variants
```sql
CREATE TABLE product_variants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL,
    barcode VARCHAR(100) NULL,
    variant_name VARCHAR(255) NOT NULL,
    base_price DECIMAL(18,2) NOT NULL,
    sale_price DECIMAL(18,2) NULL,
    compare_at_price DECIMAL(18,2) NULL,
    weight DECIMAL(10,2) NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,
    created_by VARCHAR(100) NULL,
    updated_at DATETIME NOT NULL,
    updated_by VARCHAR(100) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME NULL,
    deleted_by VARCHAR(100) NULL,
    CONSTRAINT uq_product_variants_sku UNIQUE (sku),
    CONSTRAINT fk_product_variants_product
        FOREIGN KEY (product_id) REFERENCES products(id)
);
```

### 18.3. inventories
```sql
CREATE TABLE inventories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    variant_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    on_hand INT NOT NULL DEFAULT 0,
    reserved INT NOT NULL DEFAULT 0,
    available INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uq_inventories_variant_warehouse UNIQUE (variant_id, warehouse_id),
    CONSTRAINT fk_inventories_variant
        FOREIGN KEY (variant_id) REFERENCES product_variants(id),
    CONSTRAINT fk_inventories_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);
```

### 18.4. orders
```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_code VARCHAR(50) NOT NULL,
    customer_id BIGINT NOT NULL,
    order_status VARCHAR(50) NOT NULL,
    payment_status VARCHAR(50) NOT NULL,
    shipment_status VARCHAR(50) NULL,
    subtotal_amount DECIMAL(18,2) NOT NULL,
    discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    shipping_fee DECIMAL(18,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(18,2) NOT NULL,
    voucher_code VARCHAR(100) NULL,
    voucher_discount_amount DECIMAL(18,2) NULL,
    note VARCHAR(1000) NULL,
    source VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER_WEB',
    -- Snapshot địa chỉ giao hàng
    shipping_recipient_name VARCHAR(255) NOT NULL,
    shipping_recipient_phone VARCHAR(20) NOT NULL,
    shipping_address_line VARCHAR(500) NOT NULL,
    shipping_ward VARCHAR(100) NULL,
    shipping_district VARCHAR(100) NOT NULL,
    shipping_province VARCHAR(100) NOT NULL,
    shipping_country VARCHAR(10) NOT NULL DEFAULT 'VN',
    created_at DATETIME NOT NULL,
    created_by VARCHAR(100) NULL,
    updated_at DATETIME NOT NULL,
    updated_by VARCHAR(100) NULL,
    CONSTRAINT uq_orders_order_code UNIQUE (order_code),
    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id)
);
```

### 18.5. order_items
```sql
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    variant_name VARCHAR(255) NOT NULL,
    sku VARCHAR(100) NOT NULL,
    thumbnail_url VARCHAR(500) NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    quantity INT NOT NULL,
    discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    line_total DECIMAL(18,2) NOT NULL,
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_order_items_variant
        FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);
```

### 18.6. collections
```sql
CREATE TABLE collections (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT NULL,
    image_url VARCHAR(500) NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    sort_order INT NOT NULL DEFAULT 0,
    start_at DATETIME NULL,
    end_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    created_by VARCHAR(100) NULL,
    updated_at DATETIME NOT NULL,
    updated_by VARCHAR(100) NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME NULL,
    deleted_by VARCHAR(100) NULL,
    CONSTRAINT uq_collections_slug UNIQUE (slug)
);

CREATE TABLE collection_products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    collection_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_collection_products UNIQUE (collection_id, product_id),
    CONSTRAINT fk_collection_products_collection
        FOREIGN KEY (collection_id) REFERENCES collections(id),
    CONSTRAINT fk_collection_products_product
        FOREIGN KEY (product_id) REFERENCES products(id)
);
```

---

## 19. Performance notes

### 19.1. Tránh query N+1
- dùng fetch join hợp lý
- dùng projection khi list
- không eager mọi relation

### 19.2. Không lưu dữ liệu tính toán dư thừa nếu không cần
Nhưng với các giá trị nghiệp vụ cần snapshot như order totals thì phải lưu.

### 19.3. Báo cáo nặng
Các query dashboard/report có thể:
- tối ưu index riêng
- dùng materialized approach sau này
- tách read model nếu cần

---

## 20. Backup & data safety

Production cần có:
- backup định kỳ
- retention policy
- rollback strategy
- migration review trước khi apply
- monitoring connection / slow query

---

## 21. Nguyên tắc cuối cùng

1. Database phải phản ánh đúng domain.
2. Dữ liệu giao dịch phải ưu tiên tính đúng đắn hơn tối ưu sớm.
3. Không thiết kế quá phức tạp ở MVP.
4. Nhưng các bảng cốt lõi như product_variants, order_items, payment_transactions, stock_movements phải model đúng ngay từ đầu.
5. Bảng `orders` phải snapshot đầy đủ địa chỉ giao hàng — không foreign key sang `addresses`.
6. Bảng `inventories` phải có `version` cho optimistic locking.
7. Mọi thay đổi schema phải có migration và review.
