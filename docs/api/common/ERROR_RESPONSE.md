# ERROR RESPONSE
## eProcure Enterprise — Mã lỗi & Format phản hồi lỗi

---

## 1. ERROR RESPONSE FORMAT

```jsonc
{
  "success": false,
  "code": "PR_002",                    // Mã lỗi nghiệp vụ (xem danh sách bên dưới)
  "message": "Ngân sách không đủ",     // Message tiếng Việt, dành cho người dùng
  "details": [                          // Chỉ có khi validation error, null trong các case khác
    {
      "field": "lineItems[0].quantity",
      "reason": "Số lượng phải lớn hơn 0",
      "rejectedValue": -1
    }
  ],
  "data": null,
  "meta": null,
  "timestamp": "2025-01-15T08:30:00.000Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

> ⚠️ `message` dành cho hiển thị người dùng.  
> ⚠️ `code` dùng để FE xử lý logic phân nhánh (không parse `message`).  
> ⚠️ Stack trace **KHÔNG BAO GIỜ** xuất hiện trong response production.

---

## 2. MÃ LỖI THEO SERVICE

### 2.1 Gateway — GW_

| Code | HTTP | Mô tả |
|---|---|---|
| `GW_001` | 429 | Vượt giới hạn request (rate limit) |
| `GW_002` | 401 | x-api-key không hợp lệ (inter-service) |
| `GW_003` | 503 | Service không khả dụng (circuit breaker open) |
| `GW_004` | 400 | Request malformed, không parse được |
| `GW_005` | 408 | Request timeout |

### 2.2 IAM Service — IAM_

| Code | HTTP | Mô tả |
|---|---|---|
| `IAM_001` | 401 | Username hoặc password không đúng |
| `IAM_002` | 423 | Tài khoản bị khóa (5 lần sai liên tiếp) |
| `IAM_003` | 401 | Token không hợp lệ hoặc đã bị thu hồi |
| `IAM_004` | 403 | Không có quyền thực hiện thao tác này |
| `IAM_005` | 401 | Phiên đăng nhập đã hết hạn hoặc bị invalidate |
| `IAM_006` | 401 | Mã 2FA không đúng hoặc hết hạn |
| `IAM_007` | 401 | Token reset password không hợp lệ hoặc hết hạn |
| `IAM_008` | 422 | Password mới không đủ mạnh (chính sách mật khẩu) |
| `IAM_009` | 409 | Username hoặc email đã tồn tại |
| `IAM_010` | 422 | Email chưa được xác thực |
| `IAM_011` | 400 | Dữ liệu đăng ký không hợp lệ |
| `IAM_012` | 401 | Google OAuth state/code không hợp lệ |
| `IAM_020` | 422 | Người được ủy quyền phải cùng cấp hoặc cao hơn |
| `IAM_021` | 422 | Không thể ủy quyền vượt quá quyền của bản thân |
| `IAM_022` | 409 | Đã có ủy quyền đang hoạt động trong khoảng thời gian này |
| `IAM_030` | 404 | Người dùng không tồn tại |
| `IAM_031` | 404 | Role không tồn tại |
| `IAM_032` | 404 | Permission không tồn tại |
| `IAM_033` | 404 | Phòng ban không tồn tại |
| `IAM_034` | 422 | Không tìm thấy approver phù hợp |
| `IAM_035` | 404 | Ủy quyền không tồn tại |

### 2.3 Purchase Request — PR_

| Code | HTTP | Mô tả |
|---|---|---|
| `PR_001` | 404 | Yêu cầu mua sắm không tồn tại |
| `PR_002` | 422 | Ngân sách phòng ban không đủ |
| `PR_003` | 409 | Không thể thực hiện thao tác ở trạng thái hiện tại |
| `PR_004` | 403 | Người tạo PR không được tự phê duyệt PR của mình |
| `PR_005` | 422 | Phòng ban đã vượt giới hạn 3 lần Emergency trong tháng |
| `PR_006` | 422 | Lý do khẩn cấp phải ít nhất 100 ký tự |
| `PR_007` | 404 | File đính kèm không tồn tại hoặc đã hết hạn |
| `PR_008` | 422 | File vượt quá kích thước tối đa 10MB |
| `PR_009` | 422 | Loại file không được phép |
| `PR_010` | 400 | Số lượng phải lớn hơn 0 |
| `PR_011` | 400 | Đơn giá không hợp lệ (phải >= 0) |
| `PR_012` | 400 | PR phải có ít nhất 1 mục hàng hóa |
| `PR_013` | 400 | Ngày cần nhận hàng không được trong quá khứ |
| `PR_014` | 422 | Lý do mua hàng phải ít nhất 50 ký tự |
| `PR_015` | 422 | Item từ catalog không tồn tại hoặc không còn hoạt động |

### 2.4 Approval Engine — APR_

| Code | HTTP | Mô tả |
|---|---|---|
| `APR_001` | 403 | Xung đột lợi ích: approver không thể duyệt request này |
| `APR_002` | 403 | Không phải approver được chỉ định cho task này |
| `APR_003` | 409 | Task đã được xử lý trước đó (idempotency) |
| `APR_004` | 404 | Approval task không tồn tại |
| `APR_005` | 422 | Comment bắt buộc khi Từ chối (tối thiểu 20 ký tự) |
| `APR_006` | 422 | Không thể forward cho người có cấp thấp hơn |
| `APR_007` | 422 | Approval rule không tồn tại hoặc không còn hoạt động |
| `APR_008` | 409 | Approval process đã hoàn thành, không thể thay đổi |
| `APR_010` | 422 | Approval rule conditions bị conflict với rule khác |

### 2.5 Finance Service — FIN_

| Code | HTTP | Mô tả |
|---|---|---|
| `FIN_001` | 404 | Ngân sách không tồn tại |
| `FIN_002` | 422 | 3-Way Match thất bại: số lượng không khớp |
| `FIN_003` | 422 | 3-Way Match thất bại: đơn giá không khớp |
| `FIN_004` | 403 | Vượt ngân sách > 30%, cần CEO phê duyệt |
| `FIN_005` | 422 | Ngân sách chưa được phê duyệt (status != ACTIVE) |
| `FIN_006` | 404 | Purchase Order không tồn tại |
| `FIN_007` | 404 | Hóa đơn không tồn tại |
| `FIN_008` | 409 | Hóa đơn đã được xử lý |
| `FIN_009` | 422 | Phòng ban nguồn không đủ ngân sách để chuyển |
| `FIN_010` | 422 | Số tiền hóa đơn vượt quá số tiền PO cho phép |

### 2.6 Inventory Service — INV_

| Code | HTTP | Mô tả |
|---|---|---|
| `INV_001` | 404 | Item (catalog) không tồn tại |
| `INV_002` | 404 | Kho hàng không tồn tại |
| `INV_003` | 422 | Số lượng tồn kho không đủ để cấp phát |
| `INV_004` | 404 | Phiếu nhận hàng (GR) không tồn tại |
| `INV_005` | 409 | GR đã được hoàn tất, không thể chỉnh sửa |
| `INV_006` | 422 | Số lượng nhận thực tế không thể vượt số lượng đặt hàng > 10% |
| `INV_007` | 422 | Item code đã tồn tại |

### 2.7 Vendor Service — VND_

| Code | HTTP | Mô tả |
|---|---|---|
| `VND_001` | 404 | Nhà cung cấp không tồn tại |
| `VND_002` | 409 | Mã số thuế đã tồn tại |
| `VND_003` | 422 | Nhà cung cấp đang trong danh sách đen |
| `VND_004` | 404 | RFQ không tồn tại |
| `VND_005` | 409 | RFQ đã đóng hoặc đã có kết quả |
| `VND_006` | 422 | Chưa có báo giá nào để chọn |
| `VND_007` | 422 | Thời hạn nộp báo giá đã hết |

### 2.8 Notification — NTF_

| Code | HTTP | Mô tả |
|---|---|---|
| `NTF_001` | 404 | Thông báo không tồn tại |
| `NTF_002` | 422 | Template thông báo không tồn tại |

### 2.9 System — SYS_

| Code | HTTP | Mô tả |
|---|---|---|
| `SYS_001` | 500 | Lỗi hệ thống không xác định |
| `SYS_002` | 503 | Dịch vụ phụ thuộc không khả dụng (DB, Redis, Kafka) |
| `SYS_003` | 500 | Lỗi mã hóa/giải mã payload |
| `SYS_004` | 400 | Payload đã mã hóa không hợp lệ hoặc bị giả mạo |
| `SYS_005` | 400 | Idempotency-Key header bắt buộc nhưng thiếu hoặc sai định dạng |

### 2.10 Validation — VAL_

| Code | HTTP | Mô tả |
|---|---|---|
| `VAL_001` | 400 | Dữ liệu không hợp lệ (xem `details` để biết chi tiết field) |
| `VAL_002` | 400 | JSON không đúng format |
| `VAL_003` | 400 | Query parameter không hợp lệ |

---

## 3. ERROR HANDLING RULES

### 3.1 Phân loại lỗi

```
4xx → Lỗi của client (FE có thể xử lý, hiển thị cho user)
5xx → Lỗi hệ thống (FE chỉ hiển thị generic message, alert team)

422 Unprocessable Entity → Nghiệp vụ từ chối (dùng cho business validation)
400 Bad Request         → Dữ liệu sai format/kiểu (dùng cho technical validation)
403 Forbidden           → Không đủ quyền (KHÔNG tiết lộ tại sao nếu sensitive)
```

### 3.2 Không bao giờ lộ trong response

- Stack trace Java
- Tên class / package nội bộ
- SQL query hoặc DB error message gốc
- Biến môi trường hoặc cấu hình hệ thống
- Thông tin server (hostname, IP nội bộ)
- Token, session ID, API key

### 3.3 Frontend xử lý lỗi

```typescript
// Angular — xử lý lỗi tập trung trong interceptor
if (!response.success) {
  switch (response.code) {
    case 'IAM_003':
    case 'IAM_005':
      // Token hết hạn → redirect login
      this.authService.logout();
      break;
    case 'IAM_004':
      // Không có quyền → hiển thị 403 page
      this.router.navigate(['/forbidden']);
      break;
    case 'GW_001':
      // Rate limit → hiển thị toast + retry sau
      this.toast.warning('Vui lòng thử lại sau 30 giây');
      break;
    default:
      // Business error → hiển thị message từ response
      this.toast.error(response.message);
  }
}
```
