## ADR-008 — PostgreSQL Multi-Schema, Multi-Database

**Status:** Accepted  
**Date:** 2025-01  

### Bối cảnh
Cần tách data giữa các service, không dùng schema `public` mặc định (security best practice). Cần index hợp lý cho performance.

### Quyết định

**Database phân chia:**
- Mỗi bounded context có **database riêng** (isolation cao nhất)
- Trong database, dùng schema có tên rõ ràng (không phải `public`)
- Mỗi service chỉ connect đến database của mình

**Flyway migration:**
- Mỗi service có folder `resources/db/migration/` riêng
- Naming: `V{version}__{description}.sql` (VD: `V1__create_users_table.sql`)
- Không dùng `public` schema trong bất kỳ migration nào

**Index strategy:**
```sql
-- Luôn index FK columns
CREATE INDEX idx_pr_requester_id ON pr.purchase_requests(requester_id);
-- Index cho soft delete queries
CREATE INDEX idx_pr_is_deleted ON pr.purchase_requests(is_deleted) WHERE is_deleted = false;
-- Composite index cho common queries
CREATE INDEX idx_pr_dept_status ON pr.purchase_requests(department_id, status) WHERE is_deleted = false;
-- Index cho timestamp-based queries
CREATE INDEX idx_pr_created_at ON pr.purchase_requests(created_at DESC);
```

### Hậu quả
- (+) Strong isolation giữa services
- (+) Mỗi database có thể scale/backup độc lập
- (-) Cross-service query phải qua API (không JOIN cross-DB)
- (-) Phức tạp hơn khi setup local dev → giải quyết bằng init script