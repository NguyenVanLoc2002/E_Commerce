# Order Lifecycle

Tài liệu mô tả vòng đời đơn hàng và rule chuyển trạng thái.

---

## 1. Order Status

```text
PENDING
AWAITING_PAYMENT
CONFIRMED
PROCESSING
SHIPPED
DELIVERED
COMPLETED
CANCELLED
REFUNDED
```

---

## 2. State Machine

Dưới đây là bảng chuyển trạng thái hợp lệ:

| Trạng thái hiện tại     | Trạng thái chuyển tiếp hợp lệ                  |
|-------------------------|------------------------------------------------|
| **PENDING**             | AWAITING_PAYMENT, CANCELLED                    |
| **AWAITING_PAYMENT**    | CONFIRMED, CANCELLED                           |
| **CONFIRMED**           | PROCESSING, CANCELLED                          |
| **PROCESSING**          | SHIPPED                                        |
| **SHIPPED**             | DELIVERED                                      |
| **DELIVERED**           | COMPLETED                                      |
| **COMPLETED**           | REFUNDED                                       |

**Lưu ý**: Không cho phép chuyển ngược hoặc bỏ qua trạng thái.

---

## 3. Rule quan trọng

❗ **Không được phép**:

- Chuyển trạng thái ngược về trạng thái trước
- Skip qua các trạng thái trung gian
- Chuyển trạng thái khi điều kiện payment chưa thỏa mãn

---

## 4. Payment dependency

`CONFIRMED` chỉ được chuyển khi thỏa mãn điều kiện sau:

- **COD**: Luôn cho phép
- **Online Payment**: Phải có `payment_status = PAID`

---

## 5. Inventory dependency

| Sự kiện                  | Hành động với Inventory          |
|--------------------------|----------------------------------|
| Tạo order                | Reserve stock                    |
| Cancel order             | Release stock                    |
| Complete order           | Commit stock (giảm tồn thực tế)  |

---

## 6. API design

Theo API convention:

```text
POST /api/v1/orders/{id}/confirm
POST /api/v1/orders/{id}/cancel
POST /api/v1/orders/{id}/complete
```

---

## 7. Business validation

- `ORDER_NOT_FOUND`
- `ORDER_STATUS_INVALID`
- `ORDER_CANNOT_CANCEL`

---

## 8. Transaction boundary

Service phải đảm bảo **3 thao tác** nằm trong cùng một transaction:

- Update order status
- Update inventory
- Update payment (nếu có)

---

## 9. Audit log

Ghi log tất cả các hành động quan trọng:

- Thay đổi trạng thái đơn hàng
- Hủy đơn hàng
- Hoàn tiền (refund)

---

## 10. Anti-pattern cần tránh

❌ Update trực tiếp `status` trong repository hoặc DB  
❌ Bỏ qua validation state machine  
❌ Không đồng bộ inventory khi thay đổi trạng thái

---

## 11. Future improvement

- Triển khai State Machine Engine
- Sử dụng Event-driven architecture (Kafka)
- Áp dụng Saga pattern cho payment + inventory

---