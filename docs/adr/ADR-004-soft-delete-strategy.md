## ADR-004 — Soft Delete Toàn Hệ Thống (Không có HTTP DELETE)

**Status:** Accepted  
**Date:** 2025-01  

### Bối cảnh
Yêu cầu kiểm toán: mọi dữ liệu phải có audit trail. Hard delete mất dữ liệu lịch sử, không tuân thủ SOC2/ISO27001.

### Quyết định
- **Không có** HTTP `DELETE` method trong bất kỳ API nào
- Tất cả "xoá" sử dụng: `is_deleted = true` + `deleted_at = timestamp` + `deleted_by = userId`
- Query mặc định luôn filter `WHERE is_deleted = false`
- MyBatis ResultMap dùng `@Results` để tự động filter

```sql
-- Thay vì DELETE
UPDATE pr.purchase_requests 
SET is_deleted = true, deleted_at = NOW(), deleted_by = :userId
WHERE id = :id;

-- Query mặc định
SELECT * FROM pr.purchase_requests WHERE is_deleted = false;
```

### Hậu quả
- (+) Toàn bộ lịch sử được giữ nguyên, audit-ready
- (+) Khôi phục dữ liệu dễ dàng nếu xoá nhầm
- (-) Table size tăng theo thời gian → cần archive policy
- **Giải pháp:** Partition table theo year, archive data > 5 năm sang cold storage