## SK-05 · Flyway Migration Script

### Trigger
Agent tạo hoặc sửa database schema.

### Inputs Required
- Version number + description
- Service name + schema
- Table/column definitions
- Index/constraint requirements

### Rules
```
[R1] Tên file: V{N}__{description}.sql — số thứ tự tăng dần, không trùng
[R2] Không bao giờ sửa migration file đã chạy — chỉ tạo file mới
[R3] Không dùng schema public — đặt search_path = {schema}
[R4] Mọi table có: id (UUID PK), is_deleted, deleted_at, deleted_by, created_at, created_by, updated_at, updated_by
[R5] Timestamps: TIMESTAMPTZ (không phải TIMESTAMP)
[R6] Tiền tệ: NUMERIC(19,4) — không phải DECIMAL không có precision
[R7] Index đặt tên: idx_{table}_{columns}
[R8] FK constraint đặt tên: fk_{table}_{ref_table}
[R9] Enum trong DB: VARCHAR + CHECK CONSTRAINT (không phải PostgreSQL ENUM type vì khó ALTER)
[R10] Comment bắt buộc trên mỗi CREATE TABLE
[R11] Rollback plan viết trong file V{N}__rollback_{description}.sql (backup, không auto-run)
```

### Template
```sql
-- ============================================================
-- Migration: V{N}__{description}
-- Service: {service-name}
-- Schema: {schema}
-- Author: {author}
-- Date: {YYYY-MM-DD}
-- Description: {Mô tả thay đổi}
-- ============================================================

SET search_path = {schema};

-- ----------------------------------------------------------
-- Table: {table_name}
-- ----------------------------------------------------------
CREATE TABLE {schema}.{table_name} (

    -- Identity
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    {entity}_number     VARCHAR(20)     NOT NULL,

    -- Core fields
    requester_id        UUID            NOT NULL,
    department_id       UUID            NOT NULL,
    status              VARCHAR(30)     NOT NULL,
    priority            VARCHAR(20)     NOT NULL DEFAULT 'NORMAL',
    estimated_total     NUMERIC(19,4)   NOT NULL DEFAULT 0,
    description         TEXT,
    justification       TEXT,

    -- Audit fields (required on every table)
    created_by          UUID            NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_by          UUID,
    updated_at          TIMESTAMPTZ,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID,

    -- Constraints
    CONSTRAINT pk_{table_name}           PRIMARY KEY (id),
    CONSTRAINT uq_{table_name}_number    UNIQUE ({entity}_number),
    CONSTRAINT ck_{table_name}_status    CHECK (status IN (
        'DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED',
        'CANCELLED', 'PO_ISSUED', 'COMPLETED'
    )),
    CONSTRAINT ck_{table_name}_priority  CHECK (priority IN ('NORMAL', 'URGENT', 'EMERGENCY')),
    CONSTRAINT ck_{table_name}_total     CHECK (estimated_total >= 0)
);

COMMENT ON TABLE {schema}.{table_name} IS '{Entity} — {mô tả ngắn}';
COMMENT ON COLUMN {schema}.{table_name}.estimated_total IS 'VND, NUMERIC(19,4)';

-- ----------------------------------------------------------
-- Indexes
-- ----------------------------------------------------------
CREATE INDEX idx_{table_name}_requester    ON {schema}.{table_name} (requester_id) WHERE is_deleted = false;
CREATE INDEX idx_{table_name}_dept_status  ON {schema}.{table_name} (department_id, status) WHERE is_deleted = false;
CREATE INDEX idx_{table_name}_created_at   ON {schema}.{table_name} (created_at DESC) WHERE is_deleted = false;
CREATE INDEX idx_{table_name}_status       ON {schema}.{table_name} (status) WHERE is_deleted = false;

-- ----------------------------------------------------------
-- Foreign Keys (cross-schema chỉ enforce ở application layer)
-- ----------------------------------------------------------
-- FK to same schema only — cross-service FK là application-level constraint

-- ----------------------------------------------------------
-- Seed data (nếu cần)
-- ----------------------------------------------------------
-- INSERT INTO {schema}.{table_name} ...
```

### Checklist
```
[ ] File name đúng format V{N}__{description}.sql
[ ] search_path = {schema}
[ ] Có audit fields (is_deleted, deleted_at, deleted_by, created_by, created_at)
[ ] Index có WHERE is_deleted = false
```
