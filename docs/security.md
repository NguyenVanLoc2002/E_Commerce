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
* **Refresh token**: long-lived (thời gian sống dài, ví dụ: 30 ngày).
* **Refresh rotation**: mỗi lần refresh phải tạo refresh token mới, token cũ bị vô hiệu hóa.
* **Token blacklist**: dùng Redis lưu danh sách access token đã logout hoặc bị revoke trước khi hết hạn.

---

## 3. Password

* Bắt buộc hash bằng **BCrypt** hoặc **Argon2**.
* Tuyệt đối không lưu mật khẩu dưới dạng plain text.
* Yêu cầu mật khẩu tối thiểu: ≥ 8 ký tự, có chữ hoa, chữ thường, số.

---

## 4. Authorization

Sử dụng cơ chế **RBAC** (Role-Based Access Control) với các quyền:
* **SUPER_ADMIN**: toàn quyền hệ thống
* **ADMIN**: quản trị catalog, đơn hàng, khuyến mãi
* **STAFF**: xử lý đơn hàng, kho
* **CUSTOMER**: nghiệp vụ mua hàng

---

## 5. Email Verification & OTP

### 5.1. Email verification
Sau khi đăng ký, gửi email xác minh.  
Tài khoản chưa xác minh email có thể bị giới hạn quyền (tùy business rule).

### 5.2. OTP
OTP phải:
- Lưu trong **Redis** với TTL (ví dụ: 5 phút), **không** lưu trong database
- Là chuỗi 6 chữ số ngẫu nhiên
- Chỉ dùng được 1 lần (xóa khỏi Redis sau khi verify thành công)
- Giới hạn số lần nhập sai (ví dụ: max 5 lần, sau đó tạo OTP mới)

### 5.3. Forgot password flow
1. Customer nhập email → gửi OTP về email
2. Customer nhập OTP → server verify → trả `reset_token` (short-lived, lưu Redis)
3. Customer nhập mật khẩu mới kèm `reset_token` → đổi mật khẩu thành công

---

## 6. API Protection

### Public API
* Login / Register
* Forgot password / Reset password / Verify email
* Product listing, product detail
* Category listing, brand listing, collection listing

### Protected API (Customer)
* Cart, Wishlist, Address
* Checkout, Order history
* Review

### Admin API
* Giới hạn trong pattern: `/api/v1/admin/**`
* Bắt buộc role ADMIN hoặc STAFF

---

## 7. Rate Limiting

### 7.1. Các endpoint cần rate limit

| Endpoint                         | Giới hạn gợi ý         |
|----------------------------------|------------------------|
| `POST /api/v1/auth/login`        | 10 requests / phút / IP |
| `POST /api/v1/auth/register`     | 5 requests / phút / IP  |
| `POST /api/v1/auth/forgot-password` | 3 requests / phút / IP |
| `POST /api/v1/webhooks/**`       | Whitelist IP provider   |
| `POST /api/v1/payments/**`       | 20 requests / phút / user |

### 7.2. Implement

Phase đầu: dùng Redis + Spring interceptor hoặc Bucket4j.  
Response khi bị rate limit: `429 Too Many Requests` với error code `RATE_LIMIT_EXCEEDED`.

---

## 8. Validation

* Validate input DTO chặt chẽ ở tầng Controller.
* Không tin tưởng bất kỳ input nào từ phía client.
* Áp dụng các biện pháp chống SQL injection (dùng JPA Parameterized Query, không string concat SQL).
* Chống XSS: sanitize output nếu cần render HTML.

---

## 9. File Upload Security

* Whitelist content type: `image/jpeg`, `image/png`, `image/webp`
* Giới hạn dung lượng file (ví dụ: max 5MB ảnh product)
* Sanitize / rename filename về UUID trước khi lưu
* Không cho upload file thực thi: `.exe`, `.php`, `.sh`, `.js` server-side
* Lưu file ngoài webroot, không cho truy cập direct URL nếu cần bảo mật

---

## 10. Sensitive Data

Tuyệt đối **không log** các thông tin nhạy cảm:
* Password
* JWT Token / Refresh Token
* OTP
* Payment card number / CVV
* Payment callback payload chứa thông tin nhạy cảm (chỉ log transaction ID)

---

## 11. Error Handling

Tuân thủ theo API convention:
* Không bao giờ trả `stacktrace` về phía client.
* Trả error theo cấu trúc chuẩn đã quy định.
* Log đầy đủ nội bộ để debug, nhưng không lộ ra response.

---

## 12. CORS

* Cấu hình (config) nghiêm ngặt theo từng environment.
* Tuyệt đối không sử dụng dấu `*` (allow all) ở môi trường production.
* Whitelist origin rõ ràng:
  - dev: `http://localhost:3000`, `http://localhost:5173`, `http://localhost:8081`
  - prod: chỉ domain thực tế của admin web và customer web

---

## 13. Payment Webhook Security

* Mọi request từ payment gateway phải validate **chữ ký (signature/HMAC)** trước khi xử lý.
* Secret key dùng để verify signature phải lưu trong environment variable, không hardcode.
* Nếu signature không hợp lệ: từ chối ngay với `400 Bad Request`, log cảnh báo.
* Whitelist IP của provider nếu provider cung cấp danh sách IP cố định.

---

## 14. Rate Limit cho Auth Brute Force

* Sau N lần login sai liên tiếp (ví dụ: 5 lần), khóa account tạm thời hoặc yêu cầu CAPTCHA.
* Đếm số lần sai theo `email + IP`.
* Dùng Redis để lưu counter với TTL.

---

## 15. Audit Log

Bắt buộc ghi log lịch sử đối với các hành động:
* Đăng nhập thành công / thất bại
* Đổi mật khẩu
* Thao tác của Admin (Admin action)
* Thanh toán
* Thay đổi trạng thái đơn hàng
* Chỉnh sửa tồn kho
* Tạo/sửa/vô hiệu hóa voucher

---

## 16. Common Attack Cần Chống

Hệ thống cần có cơ chế phòng chống:
* **SQL Injection**: dùng Parameterized Query / JPA
* **XSS (Cross-Site Scripting)**: sanitize input/output HTML
* **CSRF (Cross-Site Request Forgery)**: dùng SameSite cookie hoặc CSRF token nếu dùng session
* **Brute force**: rate limit + account lockout
* **Replay Attack**: idempotency key cho payment
* **Path Traversal**: sanitize filename upload

---

## 17. Security Checklist

- [ ] JWT config đúng (secret đủ mạnh, expiration hợp lý)
- [ ] Refresh token rotation
- [ ] Password hash (BCrypt/Argon2)
- [ ] OTP lưu Redis, TTL đúng
- [ ] Email verification flow
- [ ] Input validation
- [ ] Role check trên mọi protected endpoint
- [ ] Rate limit cho auth endpoints
- [ ] Payment webhook signature validation
- [ ] CORS đúng theo environment
- [ ] File upload security
- [ ] Audit log
- [ ] Không expose sensitive data trong log / response
