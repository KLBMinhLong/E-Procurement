-- V1__create_approval_foundation.sql
-- Migration: Create Approval Engine foundation tables.
-- Author: Codex
-- Date: 2026-05-27

CREATE SCHEMA IF NOT EXISTS approval;
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE OR REPLACE FUNCTION approval.update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE approval.approval_rules (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_name       VARCHAR(100)    NOT NULL,
    priority        SMALLINT        NOT NULL DEFAULT 100,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    rule_type       VARCHAR(30)     NOT NULL,
    min_value       NUMERIC(19,4),
    max_value       NUMERIC(19,4),
    categories      VARCHAR(50)[],
    department_ids  UUID[],
    priorities      VARCHAR(20)[],
    description     TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by      UUID            NOT NULL,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID,

    CONSTRAINT chk_approval_rules_type CHECK (rule_type IN ('VALUE','CATEGORY','DEPARTMENT','DEFAULT')),
    CONSTRAINT chk_approval_rules_value_range CHECK (
        min_value IS NULL OR max_value IS NULL OR max_value > min_value
    )
);

CREATE TABLE approval.approval_rule_steps (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id         UUID            NOT NULL REFERENCES approval.approval_rules(id),
    step_index      SMALLINT        NOT NULL,
    required_permission   VARCHAR(50)     NOT NULL,
    step_type       VARCHAR(20)     NOT NULL DEFAULT 'SEQUENTIAL',
    sla_hours       SMALLINT        NOT NULL,
    is_required     BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by      UUID            NOT NULL,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID,

    CONSTRAINT chk_approval_rule_steps_index CHECK (step_index > 0),
    CONSTRAINT chk_approval_rule_steps_type CHECK (step_type IN ('SEQUENTIAL','PARALLEL')),
    CONSTRAINT chk_approval_rule_steps_sla CHECK (sla_hours > 0)
);

CREATE TABLE approval.approval_processes (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type                 VARCHAR(50)     NOT NULL,
    entity_id                   UUID            NOT NULL,
    entity_number               VARCHAR(50)     NOT NULL,
    entity_title                VARCHAR(500)    NOT NULL,
    requester_id                UUID            NOT NULL,
    requester_department_id     UUID            NOT NULL,
    total_amount                NUMERIC(19,4)   NOT NULL,
    currency                    VARCHAR(3)      NOT NULL DEFAULT 'VND',
    priority                    VARCHAR(20)     NOT NULL,
    camunda_process_instance_id VARCHAR(100)    UNIQUE,
    status                      VARCHAR(30)     NOT NULL DEFAULT 'RUNNING',
    current_step_index          SMALLINT        NOT NULL DEFAULT 1,
    entity_snapshot             JSONB,
    started_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    completed_at                TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by                  UUID            NOT NULL,
    is_deleted                  BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at                  TIMESTAMPTZ,
    deleted_by                  UUID,

    CONSTRAINT chk_approval_processes_status CHECK (status IN ('RUNNING','COMPLETED','CANCELLED')),
    CONSTRAINT chk_approval_processes_entity_type CHECK (entity_type IN ('PURCHASE_REQUEST','INVOICE')),
    CONSTRAINT chk_approval_processes_priority CHECK (priority IN ('NORMAL','URGENT','EMERGENCY'))
);

CREATE TABLE approval.approval_steps (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    process_id          UUID            NOT NULL REFERENCES approval.approval_processes(id),
    step_index          SMALLINT        NOT NULL,
    step_type           VARCHAR(20)     NOT NULL DEFAULT 'SEQUENTIAL',
    required_permission       VARCHAR(50)     NOT NULL,
    approver_id         UUID            NOT NULL,
    delegate_id         UUID,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    action              VARCHAR(30),
    comment             TEXT,
    sla_deadline        TIMESTAMPTZ     NOT NULL,
    assigned_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    acted_at            TIMESTAMPTZ,
    is_escalated        BOOLEAN         NOT NULL DEFAULT FALSE,
    escalated_from      UUID,
    reminder_1_sent     BOOLEAN         NOT NULL DEFAULT FALSE,
    reminder_2_sent     BOOLEAN         NOT NULL DEFAULT FALSE,
    camunda_task_id     VARCHAR(100),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by          UUID            NOT NULL,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID,

    CONSTRAINT chk_approval_steps_index CHECK (step_index > 0),
    CONSTRAINT chk_approval_steps_type CHECK (step_type IN ('SEQUENTIAL','PARALLEL')),
    CONSTRAINT chk_approval_steps_status CHECK (
        status IN ('PENDING','APPROVED','REJECTED','ESCALATED','SKIPPED','FORWARDED')
    ),
    CONSTRAINT chk_approval_steps_action CHECK (
        action IN ('APPROVE','REJECT','REQUEST_CHANGES','FORWARD') OR action IS NULL
    )
);

CREATE TABLE approval.event_processing_log (
    event_id        VARCHAR(100)    PRIMARY KEY,
    topic           VARCHAR(150)    NOT NULL,
    partition_id    INTEGER,
    offset_value    BIGINT,
    processed_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    handler_name    VARCHAR(150)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PROCESSED',
    error_code      VARCHAR(50),
    error_message   TEXT,

    CONSTRAINT chk_event_processing_log_status CHECK (status IN ('PROCESSED','FAILED','SKIPPED'))
);

CREATE UNIQUE INDEX idx_approval_rules_name_active
    ON approval.approval_rules(rule_name)
    WHERE is_deleted = FALSE;
CREATE INDEX idx_approval_rules_active
    ON approval.approval_rules(priority DESC)
    WHERE is_active = TRUE AND is_deleted = FALSE;
CREATE INDEX idx_approval_rules_type_active
    ON approval.approval_rules(rule_type, priority DESC)
    WHERE is_active = TRUE AND is_deleted = FALSE;

CREATE UNIQUE INDEX idx_approval_rule_steps_unique_active
    ON approval.approval_rule_steps(rule_id, step_index, required_permission)
    WHERE is_deleted = FALSE;
CREATE INDEX idx_approval_rule_steps_rule
    ON approval.approval_rule_steps(rule_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_approval_processes_entity
    ON approval.approval_processes(entity_type, entity_id)
    WHERE is_deleted = FALSE;
CREATE INDEX idx_approval_processes_running
    ON approval.approval_processes(status)
    WHERE status = 'RUNNING' AND is_deleted = FALSE;
CREATE UNIQUE INDEX idx_approval_processes_entity_running
    ON approval.approval_processes(entity_type, entity_id)
    WHERE status = 'RUNNING' AND is_deleted = FALSE;

CREATE INDEX idx_approval_steps_approver_pending
    ON approval.approval_steps(approver_id, status)
    WHERE status = 'PENDING' AND is_deleted = FALSE;
CREATE INDEX idx_approval_steps_sla_pending
    ON approval.approval_steps(sla_deadline)
    WHERE status = 'PENDING' AND is_deleted = FALSE;
CREATE INDEX idx_approval_steps_process
    ON approval.approval_steps(process_id)
    WHERE is_deleted = FALSE;
CREATE INDEX idx_event_processing_log_processed
    ON approval.event_processing_log(processed_at DESC);

CREATE TRIGGER trg_approval_rules_updated_at
    BEFORE UPDATE ON approval.approval_rules
    FOR EACH ROW
    EXECUTE FUNCTION approval.update_updated_at();

CREATE TRIGGER trg_approval_rule_steps_updated_at
    BEFORE UPDATE ON approval.approval_rule_steps
    FOR EACH ROW
    EXECUTE FUNCTION approval.update_updated_at();

CREATE TRIGGER trg_approval_processes_updated_at
    BEFORE UPDATE ON approval.approval_processes
    FOR EACH ROW
    EXECUTE FUNCTION approval.update_updated_at();

CREATE TRIGGER trg_approval_steps_updated_at
    BEFORE UPDATE ON approval.approval_steps
    FOR EACH ROW
    EXECUTE FUNCTION approval.update_updated_at();

COMMENT ON TABLE approval.approval_rules IS 'Approval rule definitions selected by value, category, department, priority or default fallback.';
COMMENT ON TABLE approval.approval_rule_steps IS 'Ordered required-permission step templates for each approval rule.';
COMMENT ON TABLE approval.approval_processes IS 'Runtime approval process snapshot for business entities such as purchase requests.';
COMMENT ON TABLE approval.approval_steps IS 'Resolved approval tasks assigned to approvers or delegates.';
COMMENT ON TABLE approval.event_processing_log IS 'Idempotency log for Kafka event consumers in approval-service.';
COMMENT ON COLUMN approval.approval_rules.max_value IS 'Exclusive upper bound for value rules.';
COMMENT ON COLUMN approval.approval_processes.entity_snapshot IS 'Approval-time JSONB snapshot of the source entity context.';
