## SK-16 · Flyway Rollback Plan

### Trigger
Agent cần viết rollback plan cho migration mới.

### Inputs Required
- Migration version + description
- Schema + table name
- Backup strategy

### Rules
```
[R1] Rollback plan viết trong file riêng: V{N}__rollback_{description}.sql
[R2] Không bao giờ sửa migration đã chạy — chỉ thêm rollback file mới
[R3] Rollback plan là hướng dẫn thủ công — không tự động chạy
[R4] Luôn có bước backup dữ liệu trước khi rollback
[R5] Không dùng schema public — đặt search_path = {schema}
[R6] Mọi thao tác DROP/ALTER phải có IF EXISTS
```

### Template
```sql
-- ============================================================
-- Rollback Plan: V{N}__rollback_{description}
-- Service: {service-name}
-- Schema: {schema}
-- Author: {author}
-- Date: {YYYY-MM-DD}
-- Description: {Mô tả rollback}
-- NOTE: Manual execution only
-- ============================================================

SET search_path = {schema};

-- ----------------------------------------------------------
-- 1) Backup data (if needed)
-- ----------------------------------------------------------
-- CREATE TABLE {schema}.{table_name}_backup_{YYYYMMDD} AS
-- SELECT * FROM {schema}.{table_name};

-- ----------------------------------------------------------
-- 2) Reverse schema changes
-- ----------------------------------------------------------
-- DROP INDEX IF EXISTS idx_{table}_{columns};
-- ALTER TABLE {schema}.{table_name} DROP COLUMN IF EXISTS {column_name};
-- DROP TABLE IF EXISTS {schema}.{table_name};

-- ----------------------------------------------------------
-- 3) Restore data (if needed)
-- ----------------------------------------------------------
-- INSERT INTO {schema}.{table_name} SELECT * FROM {schema}.{table_name}_backup_{YYYYMMDD};
```

### Checklist
```
[ ] File name đúng format V{N}__rollback_{description}.sql
[ ] Có bước backup dữ liệu trước rollback
[ ] Dùng IF EXISTS cho DROP/ALTER
[ ] search_path đúng schema
```
