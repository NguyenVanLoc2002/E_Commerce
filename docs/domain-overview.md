# Domain Overview

Tài liệu mô tả nghiệp vụ và domain chính của hệ thống backend bán quần áo.

---

## 1. Tổng quan hệ thống

Đây là hệ thống e-commerce thời trang đa nền tảng, trong đó backend đóng vai trò trung tâm, cung cấp API cho:

- Admin Web
- Mobile App
- Các tích hợp nội bộ trong tương lai

### Mục tiêu chính
- Quản lý catalog sản phẩm thời trang
- Hỗ trợ biến thể size/màu
- Quản lý tồn kho theo biến thể
- Hỗ trợ giỏ hàng và đặt hàng
- Hỗ trợ thanh toán và theo dõi giao dịch
- Hỗ trợ voucher / coupon / promotion
- Hỗ trợ đánh giá sản phẩm
- Hỗ trợ vận hành bởi admin/staff

---

## 2. Nhóm domain chính

### 2.1. Identity & Access
- Auth
- User
- Role
- Permission
- Customer
- Address

### 2.2. Catalog
- Category
- Brand
- Product
- ProductVariant
- ProductMedia
- ProductAttribute
- ProductAttributeValue
- Collection

### 2.3. Commerce
- Cart
- CartItem
- Wishlist
- Order
- OrderItem
- Shipment
- Invoice

### 2.4. Inventory
- Warehouse
- Inventory
- InventoryReservation
- StockMovement

### 2.5. Payment
- Payment
- PaymentTransaction
- Refund

### 2.6. Promotion
- Promotion
- PromotionRule
- Voucher
- VoucherUsage

### 2.7. Engagement & Admin
- Review
- Notification
- Banner
- CMS Page
- Audit Log

---

## 3. Sản phẩm (Product)

### 3.1. Product là gì
`Product` là thực thể đại diện cho một sản phẩm bán ra trên hệ thống.

Ví dụ:
- Áo thun basic nam
- Quần jean slim fit nữ
- Áo khoác bomber unisex

### 3.2. Thông tin product nên chứa
- tên sản phẩm
- slug
- mô tả ngắn
- mô tả chi tiết
- thương hiệu
- category chính
- trạng thái hiển thị
- trạng thái publish
- tags
- metadata SEO

### 3.3. Product không nên chứa trực tiếp
Nếu sản phẩm có nhiều biến thể, product không nên là nơi lưu:
- stock tổng để bán
- sku bán thực tế
- giá cuối cùng theo từng size/màu

Những phần đó thuộc về `ProductVariant`.

---

## 4. Biến thể (Product Variant)

### 4.1. Variant là gì
Variant là đơn vị bán hàng thực tế của một product.

Ví dụ:
- Product: Áo thun basic nam
- Variant:
  - Trắng / M
  - Trắng / L
  - Đen / M
  - Đen / L

### 4.2. Variant nên chứa
- sku
- barcode
- option values
- base price
- sale price
- compare at price
- weight
- status
- inventory tracking flag

### 4.3. Tại sao variant quan trọng
Trong thương mại điện tử thời trang, khách mua theo tổ hợp:
- màu
- size

Nên:
- tồn kho phải theo variant
- giá có thể theo variant
- ảnh có thể theo variant
- order item nên tham chiếu tới variant

---

## 5. Danh mục (Category)

### 5.1. Category là gì
Category giúp phân loại sản phẩm:
- Áo
- Quần
- Váy
- Áo khoác
- Phụ kiện

### 5.2. Hỗ trợ category cha-con
Ví dụ:
- Nam
  - Áo nam
  - Quần nam
- Nữ
  - Áo nữ
  - Váy nữ

### 5.3. Vai trò của category
- điều hướng menu
- filter sản phẩm
- áp promotion theo nhóm
- hỗ trợ SEO
- hỗ trợ phân quyền quản trị nếu cần

---

## 6. Brand

### 6.1. Brand là gì
Thương hiệu của sản phẩm.

Ví dụ:
- Local brand A
- Fashion brand B

### 6.2. Brand dùng để làm gì
- lọc sản phẩm
- hiển thị thương hiệu
- phân tích doanh thu theo brand
- quản trị catalog

---

## 7. Product Media

### 7.1. ProductMedia là gì
Dùng để lưu hình ảnh/video của sản phẩm hoặc variant.

### 7.2. Có thể hỗ trợ
- ảnh cover
- ảnh gallery
- ảnh theo màu
- video ngắn giới thiệu sản phẩm

### 7.3. Quy tắc
- một product có nhiều media
- có thể gắn media ở mức variant nếu màu sắc có ảnh riêng
- có cờ `is_primary`

---

## 8. Thuộc tính sản phẩm (Product Attributes)

### 8.1. Ví dụ thuộc tính
- size
- color
- material
- fit
- gender
- sleeve type

### 8.2. Mục tiêu
- sinh biến thể
- filter
- hiển thị chi tiết sản phẩm

### 8.3. Phân loại
- attribute dùng để sinh variant: size, color
- attribute chỉ để mô tả: material, form, style

---

## 9. Tồn kho (Inventory)

### 9.1. Inventory là gì
Quản lý số lượng tồn kho theo `variant`, có thể theo từng `warehouse`.

### 9.2. Các chỉ số nên có
- `onHand`: tồn thực tế
- `reserved`: số lượng đang giữ cho đơn đang xử lý
- `available`: số lượng có thể bán

Công thức:
```text
available = onHand - reserved
```

### 9.3. InventoryReservation
Dùng để giữ hàng tạm:
- khi checkout
- khi tạo order chờ thanh toán
- khi thanh toán chưa xác nhận xong

### 9.4. StockMovement
Lịch sử thay đổi tồn kho:
- IMPORT
- EXPORT
- RESERVE
- RELEASE
- ADJUST
- RETURN

### 9.5. Quy tắc tồn kho
- không cho đặt vượt available
- khi tạo order có thể reserve stock
- khi order cancel thì release/resume stock
- khi trả hàng có thể nhập kho lại nếu nghiệp vụ cho phép

---

## 10. Giỏ hàng (Cart)

### 10.1. Cart là gì
Giỏ hàng của customer trước khi đặt hàng.

### 10.2. CartItem chứa gì
- customer
- variant
- quantity
- selected state
- unit price snapshot tạm thời

### 10.3. Quy tắc cart
- quantity > 0
- không vượt quá available
- nếu variant bị inactive/out of stock thì cần cảnh báo khi checkout
- giá cuối cùng vẫn phải tính lại khi checkout

---

## 11. Wishlist

### 11.1. Mục đích
Cho phép customer lưu sản phẩm quan tâm để xem lại sau.

### 11.2. Lưu theo gì
Khuyến nghị lưu theo product.  
Nếu business cần chính xác hơn có thể lưu theo variant.

---

## 12. Đơn hàng (Order)

### 12.1. Order là gì
Là giao dịch mua hàng của khách.

### 12.2. Order nên chứa
- order code
- customer
- order status
- payment status
- shipment status
- subtotal
- discount amount
- shipping fee
- total amount
- note
- source
- địa chỉ snapshot
- thông tin voucher/promotion đã áp

### 12.3. Order lifecycle cơ bản
- PENDING
- AWAITING_PAYMENT
- CONFIRMED
- PROCESSING
- SHIPPED
- DELIVERED
- COMPLETED
- CANCELLED
- REFUNDED

### 12.4. Order source
Có thể gồm:
- MOBILE_APP
- ADMIN_WEB
- CUSTOMER_WEB
- INTERNAL

---

## 13. Chi tiết đơn hàng (OrderItem)

### 13.1. OrderItem là gì
Mỗi dòng hàng trong đơn.

### 13.2. OrderItem bắt buộc snapshot
- product id
- variant id
- product name
- variant name
- sku
- unit price
- quantity
- discount amount
- line total

### 13.3. Tại sao cần snapshot
Để tránh việc product bị đổi tên, đổi giá, đổi biến thể làm sai lịch sử đơn hàng.

---

## 14. Giao hàng (Shipment)

### 14.1. Shipment là gì
Thông tin vận chuyển của đơn hàng.

### 14.2. Có thể bao gồm
- shipment code
- carrier
- tracking number
- shipping fee
- shipping status
- shipped at
- delivered at

### 14.3. Trạng thái shipment
- PENDING
- PACKING
- READY_TO_SHIP
- SHIPPING
- DELIVERED
- FAILED
- RETURNED

---

## 15. Hóa đơn (Invoice)

### 15.1. Invoice là gì
Bản ghi hóa đơn cho order.

### 15.2. Công dụng
- lưu chứng từ
- hiển thị cho admin
- in/xuất file
- đối soát thanh toán nếu cần

### 15.3. Có thể chứa
- invoice code
- order id
- invoice status
- invoice amount
- tax amount nếu có
- issued date

---

## 16. Thanh toán (Payment)

### 16.1. Payment là gì
Thực thể biểu diễn tình trạng thanh toán chính của order.

### 16.2. Payment nên chứa
- order id
- payment method
- payment status
- payable amount
- paid amount
- payment provider
- external reference

### 16.3. Phương thức thanh toán
Ví dụ:
- COD
- BANK_TRANSFER
- MOMO
- VNPAY
- STRIPE
- PAYPAL

---

## 17. Giao dịch thanh toán (PaymentTransaction)

### 17.1. Vì sao cần riêng
Một payment có thể có nhiều bước/giao dịch:
- tạo yêu cầu
- pending
- callback success/fail
- refund
- retry

### 17.2. PaymentTransaction nên chứa
- payment id
- transaction code
- provider transaction id
- request payload
- response payload
- transaction status
- amount
- created at

### 17.3. Trạng thái gợi ý
- INITIATED
- PENDING
- AUTHORIZED
- PAID
- FAILED
- CANCELLED
- REFUNDED
- PARTIALLY_REFUNDED

---

## 18. Promotion

### 18.1. Promotion là gì
Chương trình khuyến mãi tổng quát.

Ví dụ:
- Sale hè 20%
- Mua 2 tặng 1
- Freeship cuối tuần

### 18.2. Promotion có thể áp dụng cho
- toàn bộ shop
- category
- brand
- product
- variant
- đơn hàng đạt điều kiện

### 18.3. Thành phần của promotion
- thông tin chung
- thời gian hiệu lực
- status
- scope
- rules

---

## 19. PromotionRule

### 19.1. Là gì
Là tập điều kiện và cách tính khuyến mãi.

### 19.2. Ví dụ rule
- giảm 10% cho category áo
- giảm 50.000 cho đơn từ 500.000
- miễn phí ship cho đơn từ 300.000
- mua 2 áo tặng 1 áo

### 19.3. Lưu ý
Rule engine giai đoạn đầu nên đơn giản, dễ kiểm soát.  
Không nên làm quá phức tạp ngay từ MVP.

---

## 20. Voucher / Coupon

### 20.1. Voucher là gì
Mã giảm giá do customer nhập thủ công.

Ví dụ:
- WELCOME10
- FREESHIP
- SUMMER2026

### 20.2. Voucher có thể có điều kiện
- active/inactive
- ngày bắt đầu / kết thúc
- min order value
- max discount amount
- usage limit
- per-user limit
- áp cho product/category nhất định

### 20.3. VoucherUsage
Lưu lịch sử ai đã dùng voucher nào, cho đơn nào, vào thời điểm nào.

---

## 21. Review

### 21.1. Review là gì
Đánh giá sản phẩm bởi customer.

### 21.2. Điều kiện nên có
Chỉ cho phép review với customer đã mua sản phẩm và đơn hàng đã hoàn tất.

### 21.3. Nội dung
- rating
- comment
- media nếu có
- trạng thái duyệt nếu hệ thống cần moderation

---

## 22. Notification

### 22.1. Loại thông báo
- order created
- payment success
- order shipped
- voucher campaign
- admin notice

### 22.2. Kênh thông báo
- in-app
- email
- push notification
- SMS sau này nếu cần

---

## 23. Admin làm được gì

### 23.1. Quản trị catalog
- tạo/sửa/xóa category
- tạo/sửa/xóa brand
- tạo/sửa/xóa product
- tạo variants
- upload media
- publish/unpublish sản phẩm

### 23.2. Quản trị tồn kho
- nhập tồn
- điều chỉnh tồn
- xem lịch sử biến động tồn

### 23.3. Quản trị đơn hàng
- xem danh sách đơn
- xác nhận đơn
- cập nhật trạng thái
- tạo shipment
- hủy đơn
- xem thanh toán / refund

### 23.4. Quản trị khuyến mãi
- tạo promotion
- tạo voucher
- giới hạn thời gian / đối tượng áp dụng
- bật / tắt campaign

### 23.5. Quản trị khách hàng
- xem customer
- khóa / mở tài khoản
- xem lịch sử mua hàng

---

## 24. Customer làm được gì
- đăng ký / đăng nhập
- cập nhật profile
- quản lý địa chỉ
- xem danh sách sản phẩm
- tìm kiếm / lọc / sort
- xem chi tiết sản phẩm
- chọn size / màu
- thêm giỏ hàng
- áp voucher
- checkout
- theo dõi đơn
- đánh giá sản phẩm
- quản lý wishlist

---

## 25. Quan hệ domain chính

**Catalog**
- Category 1 - N Category
- Brand 1 - N Product
- Product 1 - N ProductVariant
- Product 1 - N ProductMedia
- Product N - N Category
- ProductVariant N - N AttributeValue

**Commerce**
- Customer 1 - N Address
- Customer 1 - 1 Cart
- Cart 1 - N CartItem
- Customer 1 - N Order
- Order 1 - N OrderItem
- Order 1 - N PaymentTransaction
- Order 1 - 1..N Shipment
- Order 1 - 0..1 Invoice

**Inventory**
- Warehouse 1 - N Inventory
- ProductVariant 1 - N Inventory
- Inventory 1 - N StockMovement

**Promotion**
- Promotion 1 - N PromotionRule
- Voucher 1 - N VoucherUsage

---

## 26. Nguyên tắc boundary giữa các domain

- **Product domain**: Quản lý catalog và thông tin sản phẩm.
- **Inventory domain**: Quản lý số lượng hàng hóa và khả năng bán.
- **Cart domain**: Quản lý item khách tạm chọn trước checkout.
- **Order domain**: Quản lý vòng đời giao dịch mua hàng.
- **Payment domain**: Quản lý thanh toán và đồng bộ trạng thái với provider.
- **Promotion domain**: Tính logic giảm giá, voucher, ưu đãi.
- **Admin domain**: Điều phối màn hình và use case quản trị, không thay thế logic domain gốc.

---

## 27. Những use case cốt lõi phải hỗ trợ

### 27.1. Catalog flow
1. Admin tạo product
2. Admin tạo variant
3. Admin upload media
4. Admin nhập tồn kho
5. Product được publish

### 27.2. Customer purchase flow
1. Customer xem product
2. Chọn variant
3. Thêm cart
4. Checkout
5. Áp voucher
6. Tạo order
7. Tạo payment
8. Thanh toán
9. Order được xác nhận

### 27.3. Order fulfillment flow
1. Admin xác nhận order
2. Reserve/commit inventory
3. Đóng gói
4. Tạo shipment
5. Giao hàng
6. Hoàn tất đơn

### 27.4. Review flow
1. Order completed
2. Customer đánh giá sản phẩm
3. Review được duyệt/hiển thị

---

## 28. Scope ưu tiên MVP

**Bắt buộc**
- auth
- user/customer
- address
- category
- brand
- product
- product variant
- inventory
- cart
- order
- payment cơ bản
- admin basic

**Nên có sớm**
- voucher
- shipment
- invoice
- review

**Có thể để sau**
- loyalty
- recommendation
- CMS nâng cao
- analytics phức tạp
- nhiều kho nâng cao

---
