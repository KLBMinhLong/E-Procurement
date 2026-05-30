CREATE TABLE IF NOT EXISTS finance.event_processing_log (
    event_id VARCHAR(80) PRIMARY KEY,
    topic VARCHAR(120) NOT NULL,
    partition_id INTEGER NULL,
    offset_value BIGINT NULL,
    handler_name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_event_processing_log_status CHECK (status IN ('PROCESSED','SKIPPED'))
);

ALTER TABLE finance.budget_transactions
    ADD COLUMN IF NOT EXISTS source_event_id VARCHAR(80) NULL;

CREATE INDEX IF NOT EXISTS ix_budget_transactions_source_event
    ON finance.budget_transactions (source_event_id)
    WHERE source_event_id IS NOT NULL;

COMMENT ON TABLE finance.event_processing_log IS 'Idempotency log for Kafka event handlers in finance-service.';
COMMENT ON COLUMN finance.budget_transactions.source_event_id IS 'Kafka/source event id that produced this immutable ledger row.';
