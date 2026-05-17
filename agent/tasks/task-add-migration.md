# TASK: ADD MIGRATION
## eProcure Enterprise — Playbook Tạo Flyway Migration

---

> Dùng file này khi cần tạo hoặc thay đổi schema database: table, column, index, constraint, trigger, seed permission, hoặc rollback plan.  
> Không sửa migration đã commit; tạo version mới để ALTER.

---

## 1. TRIGGER

Kích hoạt task này khi request có dạng:

```
thêm migration ...
tạo bảng ...
thêm column ...
thêm index ...
sửa schema ...
seed permission ...
Flyway ...
```

---

## 2. CONTEXT BẮT BUỘC PHẢI ĐỌC

```
AGENTS.md
docs/DATABASE_SCHEMA.md
agent/knowledge/database-schema-overview.md
agent/knowledge/soft-delete-strategy.md
agent/knowledge/timezone-financial-precision.md
agent/memory/coding-patterns.md       # Mục migration nếu có
.cursor/rules/database.mdc
agent/skills/SK-05-flyway-migration-script.md
agent/skills/SK-16-flyway-rollback-plan.md
```

Nếu thêm permission seed:

```
agent/knowledge/rbac-permission-codes.md
.cursor/rules/error-codes.mdc nếu thêm error seed liên quan
```

---

## 3. VERSIONING RULES

Tìm version cuối:

```
src/main/resources/db/migration/V*.sql
```

Đặt file mới:

```
V{N+1}__{lower_snake_description}.sql
```

Ví dụ:

```
V3__create_purchase_requests.sql
V4__add_pr_approval_indexes.sql
V5__seed_pr_permissions.sql
```

Không dùng:

```
V4_add_column.sql        # thiếu double underscore
V004__something.sql      # không theo style hiện có nếu repo không dùng padding
R__manual_patch.sql      # repeatable migration chỉ dùng khi đã có convention rõ
```

---

## 4. SCHEMA RULES

```
[ ] Không dùng schema public.
[ ] CREATE SCHEMA IF NOT EXISTS {schema};
[ ] Primary key UUID DEFAULT gen_random_uuid().
[ ] Money dùng NUMERIC(19,4), không FLOAT/DOUBLE.
[ ] Timestamp dùng TIMESTAMPTZ, không TIMESTAMP.
[ ] Mọi entity table có audit + soft delete columns.
[ ] Enum/status column có CHECK constraint.
[ ] FK columns có index.
[ ] Common query có composite/partial index.
[ ] SELECT target table phải hỗ trợ WHERE is_deleted = false.
[ ] Trigger auto-update updated_at nếu table có updated_at.
[ ] COMMENT ON TABLE và COMMENT ON COLUMN quan trọng.
```

Audit + soft delete chuẩn:

```sql
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
created_by UUID,
updated_by UUID,
is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
deleted_at TIMESTAMPTZ,
deleted_by UUID
```

Partial index chuẩn:

```sql
CREATE INDEX IF NOT EXISTS idx_purchase_requests_requester_active
    ON pr.purchase_requests (requester_id, created_at DESC)
    WHERE is_deleted = FALSE;
```

---

## 5. EXECUTION FLOW

### Bước 1: Xác định DB và schema

```
iam-service          -> db_iam / schema iam
pr-service           -> db_procurement / schema pr
approval-service     -> db_procurement / schema approval
finance-service      -> db_finance / schema finance
inventory-service    -> db_inventory / schema inventory
vendor-service       -> db_vendor / schema vendor
notification-service -> db_notification / schema notification
admin/audit          -> db_audit / schema audit
```

### Bước 2: Đối chiếu `docs/DATABASE_SCHEMA.md`

Kiểm tra:

```
- Object đã tồn tại chưa?
- Tên table/column có đúng convention không?
- Có schema ownership rõ không?
- Có overlap với service khác không?
```

### Bước 3: Viết migration

Thứ tự trong file:

```
1. CREATE SCHEMA IF NOT EXISTS
2. extension cần thiết nếu migration đầu tiên cần UUID
3. CREATE TABLE / ALTER TABLE
4. CHECK / FK / UNIQUE constraints
5. Indexes
6. Trigger function + trigger updated_at
7. Seed data nếu cần
8. COMMENT ON TABLE / COLUMN
```

### Bước 4: Rollback plan

Với migration phức tạp, thêm comment cuối file hoặc docs kèm:

```sql
-- Rollback plan:
-- 1. DROP INDEX IF EXISTS ...
-- 2. ALTER TABLE ... DROP COLUMN ...
-- 3. DROP TABLE IF EXISTS ...
```

Không tạo rollback tự động nếu dự án chưa dùng Flyway undo.

### Bước 5: Cập nhật docs

```
docs/DATABASE_SCHEMA.md
agent/knowledge/database-schema-overview.md nếu thay đổi layout lớn
agent/knowledge/rbac-permission-codes.md nếu seed permission mới
agent/memory/progress-tracker.md
```

---

## 6. OUTPUT CONTRACT

Migration hoàn chỉnh phải có:

```
src/main/resources/db/migration/V{N}__{description}.sql
docs/DATABASE_SCHEMA.md cập nhật nếu schema thay đổi
rollback note nếu migration không trivial
```

Nếu migration phục vụ endpoint/feature:

```
Repository/MyBatis query tương ứng có is_deleted = false.
Domain/Application dùng BigDecimal cho money field.
OpenAPI money field là string.
```

---

## 7. FINAL CHECKLIST

```
Naming:
[ ] File đúng V{N}__{description}.sql.
[ ] N là version tiếp theo, không trùng.

Schema:
[ ] CREATE SCHEMA IF NOT EXISTS.
[ ] Không dùng public.
[ ] UUID primary key.
[ ] NUMERIC(19,4) cho tiền.
[ ] TIMESTAMPTZ cho timestamp.
[ ] Soft delete đầy đủ.
[ ] Audit columns đầy đủ.
[ ] CHECK constraint cho enum/status.
[ ] FK/index/common query index đầy đủ.
[ ] Partial index WHERE is_deleted = FALSE khi phù hợp.
[ ] Trigger updated_at.
[ ] COMMENT ON TABLE/COLUMN.

Safety:
[ ] Không sửa migration cũ đã commit.
[ ] Không DROP dữ liệu production nếu không có chỉ định rõ.
[ ] Rollback plan có cho thay đổi rủi ro.
[ ] docs/DATABASE_SCHEMA.md cập nhật.
```
