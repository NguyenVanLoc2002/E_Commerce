# Security Guidelines

Tài liệu mô tả nguyên tắc bảo mật cho hệ thống.

---

## 1. Authentication

* **JWT access token**
* **Refresh token**

Header mẫu:
```http
Authorization: Bearer <token>
```

---

## 2. Token Strategy

* **Access token**: short-lived (thời gian sống ngắn, ví dụ: 1h).
* **Refresh token**: long-lived (thời gian sống dài).
* **Refresh rotation**: cơ chế xoay vòng refresh token để tăng tính an toàn.

---

## 3. Password

* Bắt buộc hash bằng **BCrypt** hoặc **Argon2**.
* Tuyệt đối không lưu mật khẩu dưới dạng plain text (văn bản thô).

---

## 4. Authorization

Sử dụng cơ chế **RBAC** (Role-Based Access Control) với các quyền:
* **ADMIN**
* **STAFF**
* **CUSTOMER**

---

## 5. API Protection

### Public API
* Login / Register (Đăng nhập / Đăng ký)
* Product listing (Danh sách sản phẩm)

### Protected API
* Cart (Giỏ hàng)
* Order (Đơn hàng)

### Admin API
* Giới hạn trong pattern: `/api/v1/admin/**`

---

## 6. Validation

* Validate input DTO chặt chẽ ở tầng Controller.
* Áp dụng các biện pháp chống injection.

---

## 7. File Upload Security

* Check content type (kiểm tra định dạng file hợp lệ).
* Limit size (giới hạn dung lượng file tải lên).
* Sanitize filename (làm sạch tên file để tránh path traversal).

---

## 8. Sensitive Data

Tuyệt đối **không log** các thông tin nhạy cảm:
* Password (Mật khẩu)
* Token (Mã định danh)
* Payment info (Thông tin thanh toán)

---

## 9. Error Handling

Tuân thủ theo API convention:
* Không bao giờ trả `stacktrace` về phía client.
* Trả error theo cấu trúc chuẩn đã quy định.

---

## 10. CORS

* Cấu hình (config) nghiêm ngặt theo từng environment (môi trường).
* Tuyệt đối không sử dụng dấu `*` (allow all) ở môi trường production.

---

## 11. Rate Limit (Future)

Dự kiến áp dụng giới hạn tần suất gọi API cho:
* API Login
* API Payment

---

## 12. Audit Log

Bắt buộc ghi log lịch sử đối với các hành động:
* Đăng nhập (Login)
* Thao tác của Admin (Admin action)
* Thanh toán (Payment)

---

## 13. Common Attack Cần Chống

Hệ thống cần có cơ chế phòng chống các cuộc tấn công phổ biến:
* SQL Injection
* XSS (Cross-Site Scripting)
* CSRF (Cross-Site Request Forgery)
* Brute force

---

## 14. Security Checklist

- [ ] JWT config đúng
- [ ] Password hash
- [ ] Input validation
- [ ] Role check
- [ ] Audit log
- [ ] Không expose sensitive data

---
