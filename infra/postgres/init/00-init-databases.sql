-- eProcure local PostgreSQL bootstrap.
-- This script is idempotent and intentionally avoids business objects in schema public.

\set ON_ERROR_STOP on

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'iam_user') THEN
        CREATE ROLE iam_user LOGIN PASSWORD 'iam_pass_dev';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'pr_user') THEN
        CREATE ROLE pr_user LOGIN PASSWORD 'pr_pass_dev';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'approval_user') THEN
        CREATE ROLE approval_user LOGIN PASSWORD 'approval_pass_dev';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'finance_user') THEN
        CREATE ROLE finance_user LOGIN PASSWORD 'finance_pass_dev';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'inventory_user') THEN
        CREATE ROLE inventory_user LOGIN PASSWORD 'inventory_pass_dev';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'vendor_user') THEN
        CREATE ROLE vendor_user LOGIN PASSWORD 'vendor_pass_dev';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'notification_user') THEN
        CREATE ROLE notification_user LOGIN PASSWORD 'notification_pass_dev';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'analytics_user') THEN
        CREATE ROLE analytics_user LOGIN PASSWORD 'analytics_pass_dev';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'audit_user') THEN
        CREATE ROLE audit_user LOGIN PASSWORD 'audit_pass_dev';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'camunda_user') THEN
        CREATE ROLE camunda_user LOGIN PASSWORD 'camunda_pass_dev';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'analytics_ro') THEN
        CREATE ROLE analytics_ro LOGIN PASSWORD 'analytics_pass_dev';
    END IF;
END $$;

SELECT 'CREATE DATABASE db_iam OWNER iam_user'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'db_iam')\gexec
SELECT 'CREATE DATABASE db_procurement OWNER pr_user'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'db_procurement')\gexec
SELECT 'CREATE DATABASE db_finance OWNER finance_user'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'db_finance')\gexec
SELECT 'CREATE DATABASE db_inventory OWNER inventory_user'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'db_inventory')\gexec
SELECT 'CREATE DATABASE db_vendor OWNER vendor_user'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'db_vendor')\gexec
SELECT 'CREATE DATABASE db_notification OWNER notification_user'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'db_notification')\gexec
SELECT 'CREATE DATABASE db_analytics OWNER analytics_user'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'db_analytics')\gexec
SELECT 'CREATE DATABASE db_audit OWNER audit_user'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'db_audit')\gexec
SELECT 'CREATE DATABASE db_camunda OWNER camunda_user'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'db_camunda')\gexec

\connect db_iam
CREATE EXTENSION IF NOT EXISTS pgcrypto;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA IF NOT EXISTS iam AUTHORIZATION iam_user;
CREATE SCHEMA IF NOT EXISTS iam_keycloak AUTHORIZATION iam_user;
GRANT CONNECT ON DATABASE db_iam TO iam_user;
GRANT USAGE, CREATE ON SCHEMA iam TO iam_user;
GRANT USAGE, CREATE ON SCHEMA iam_keycloak TO iam_user;
ALTER DATABASE db_iam SET search_path TO iam;

\connect db_procurement
CREATE EXTENSION IF NOT EXISTS pgcrypto;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA IF NOT EXISTS pr AUTHORIZATION pr_user;
CREATE SCHEMA IF NOT EXISTS approval AUTHORIZATION approval_user;
GRANT CONNECT, CREATE ON DATABASE db_procurement TO approval_user;
GRANT CONNECT ON DATABASE db_procurement TO pr_user, analytics_ro;
GRANT USAGE, CREATE ON SCHEMA pr TO pr_user;
GRANT USAGE, CREATE ON SCHEMA approval TO approval_user;
GRANT USAGE ON SCHEMA pr, approval TO analytics_ro;
ALTER DATABASE db_procurement SET search_path TO pr, approval;

\connect db_finance
CREATE EXTENSION IF NOT EXISTS pgcrypto;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA IF NOT EXISTS finance AUTHORIZATION finance_user;
GRANT CONNECT ON DATABASE db_finance TO finance_user;
GRANT USAGE, CREATE ON SCHEMA finance TO finance_user;
ALTER DATABASE db_finance SET search_path TO finance;

\connect db_inventory
CREATE EXTENSION IF NOT EXISTS pgcrypto;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA IF NOT EXISTS inventory AUTHORIZATION inventory_user;
GRANT CONNECT ON DATABASE db_inventory TO inventory_user;
GRANT USAGE, CREATE ON SCHEMA inventory TO inventory_user;
ALTER DATABASE db_inventory SET search_path TO inventory;

\connect db_vendor
CREATE EXTENSION IF NOT EXISTS pgcrypto;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA IF NOT EXISTS vendor AUTHORIZATION vendor_user;
GRANT CONNECT ON DATABASE db_vendor TO vendor_user;
GRANT USAGE, CREATE ON SCHEMA vendor TO vendor_user;
ALTER DATABASE db_vendor SET search_path TO vendor;

\connect db_notification
CREATE EXTENSION IF NOT EXISTS pgcrypto;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA IF NOT EXISTS notification AUTHORIZATION notification_user;
GRANT CONNECT ON DATABASE db_notification TO notification_user;
GRANT USAGE, CREATE ON SCHEMA notification TO notification_user;
ALTER DATABASE db_notification SET search_path TO notification;

\connect db_analytics
CREATE EXTENSION IF NOT EXISTS pgcrypto;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA IF NOT EXISTS analytics AUTHORIZATION analytics_user;
GRANT CONNECT ON DATABASE db_analytics TO analytics_user;
GRANT USAGE, CREATE ON SCHEMA analytics TO analytics_user;
ALTER DATABASE db_analytics SET search_path TO analytics;

\connect db_audit
CREATE EXTENSION IF NOT EXISTS pgcrypto;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA IF NOT EXISTS audit AUTHORIZATION audit_user;
GRANT CONNECT ON DATABASE db_audit TO audit_user;
GRANT USAGE, CREATE ON SCHEMA audit TO audit_user;
ALTER DATABASE db_audit SET search_path TO audit;

\connect db_camunda
CREATE EXTENSION IF NOT EXISTS pgcrypto;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA IF NOT EXISTS camunda AUTHORIZATION camunda_user;
GRANT CONNECT ON DATABASE db_camunda TO camunda_user;
GRANT USAGE, CREATE ON SCHEMA camunda TO camunda_user;
ALTER DATABASE db_camunda SET search_path TO camunda;
