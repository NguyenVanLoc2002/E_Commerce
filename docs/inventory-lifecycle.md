# Inventory Lifecycle

Tài liệu mô tả vòng đời tồn kho và các thao tác liên quan.

---

## 1. Inventory Model

Hệ thống quản lý tồn kho dựa trên ba chỉ số chính:

* **on_hand**: Tồn kho thực tế trong kho.
* **reserved**: Số lượng hàng đang được giữ tạm (chờ thanh toán, chờ giao).
* **available**: Số lượng thực tế có thể bán.

$$available = on\_hand - reserved$$



---

## 2. Stock Movement Types
Các loại biến động kho:
* **IMPORT**: Nhập kho.
* **EXPORT**: Xuất kho.
* **RESERVE**: Giữ hàng tạm thời.
* **RELEASE**: Giải phóng hàng đang giữ.
* **ADJUST**: Điều chỉnh kho (kiểm kê).
* **RETURN**: Hàng trả về.

---

## 3. Lifecycle Flow

### 3.1. Import stock
* Tăng `on_hand`.

### 3.2. Add to cart (Optional)
* Không giữ stock (tùy thuộc vào business rule, thường không tác động đến database ở bước này).

### 3.3. Checkout
* Tạo bản ghi reservation.
* Tăng `reserved`.

### 3.4. Order confirmed
* Giữ nguyên trạng thái `reserved`.

### 3.5. Cancel order
* Giảm `reserved` (nhả hàng về lại `available`).

### 3.6. Complete order
* Giảm `on_hand`.
* Giảm `reserved`.

---

## 4. Reservation Rule

Mỗi bản ghi reservation bao gồm:
* `variant_id`: Mã định danh sản phẩm.
* `quantity`: Số lượng giữ.
* `expires_at`: Thời gian hết hạn.

> **Lưu ý:** Nếu quá thời gian `expires_at` mà đơn hàng chưa hoàn tất → Hệ thống tự động **Release** (Auto release).

---

## 5. Validation
Các lỗi cần bắt khi xử lý tồn kho:
* `INVENTORY_NOT_ENOUGH`: Không đủ tồn kho.
* `VARIANT_OUT_OF_STOCK`: Sản phẩm đã hết hàng.

---

## 6. Concurrency Problem

**Vấn đề cần tránh:**
* **Oversell**: Bán quá số lượng tồn kho thực tế do nhiều người cùng mua một lúc.

**Giải pháp:**
* **Optimistic locking**: Sử dụng versioning.
* **Update with condition**: `WHERE available >= quantity_to_buy`.
* **Redis lock** (Optional): Sử dụng phân tán lock cho các case chịu tải cực cao.

---

## 7. API Example
* `POST /api/v1/admin/inventories/adjust`: Điều chỉnh kho thủ công.
* `POST /api/v1/orders/{id}/reserve`: Giữ hàng cho đơn hàng.
* `POST /api/v1/orders/{id}/release`: Giải phóng hàng cho đơn hàng.

---

## 8. Database Rule
Theo DB guideline:
* **Unique constraint**: `(variant_id, warehouse_id)`.
* **No Cascade Delete**: Không xóa cứng các dữ liệu liên quan đến tồn kho.
* **Stock Movements Log**: Phải có bảng log để truy vết mọi biến động.

---

## 9. Audit & Tracking
* Lưu mọi bản ghi vào bảng `stock_movements`.
* Bắt buộc lưu kèm `reason` (Lý do thay đổi: nhập hàng, bán hàng, hoàn trả...).

---

## 10. Anti-pattern
* ❌ Update trực tiếp cột `available` (phải tính toán từ `on_hand` và `reserved`).
* ❌ Không sử dụng cơ chế reservation (dẫn đến tranh chấp khi thanh toán).
* ❌ Không log movement (gây khó khăn khi đối soát kho).

---

## 11. Future Improvement
* **Multi warehouse allocation**: Phân bổ tồn kho từ nhiều kho hàng khác nhau.
* **FIFO/LIFO stock**: Quản lý xuất nhập kho theo phương pháp nhập trước xuất trước hoặc nhập sau xuất trước.
* **Batch tracking**: Theo dõi hàng hóa theo lô sản xuất và hạn sử dụng.