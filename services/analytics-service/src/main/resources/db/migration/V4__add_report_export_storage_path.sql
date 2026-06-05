CREATE SCHEMA IF NOT EXISTS analytics;

ALTER TABLE analytics.report_export_jobs
    ADD COLUMN IF NOT EXISTS storage_path TEXT NULL;

COMMENT ON COLUMN analytics.report_export_jobs.storage_path IS 'Local object/file path used by analytics report export worker.';
