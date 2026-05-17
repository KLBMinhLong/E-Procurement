# Database Schema Overview

## Rules
- No public schema
- Each service owns its database/schema
- Soft delete fields on all entity tables
- Timestamps: TIMESTAMPTZ (UTC)
- Money: NUMERIC(19,4)
- Flyway migrations: V{N}__{description}.sql

## Database layout
- db_iam (schema iam): users, roles, permissions, user_roles, role_permissions,
  departments, org_nodes, delegations, sessions
- db_procurement (schema pr): purchase_requests, pr_line_items, pr_attachments,
  catalog_items, catalog_categories
- db_procurement (schema approval): approval_processes, approval_steps,
  approval_rules, approval_rule_steps
- db_finance (schema finance): budgets, budget_transactions, budget_transfers,
  purchase_orders, po_line_items, invoices, invoice_line_items, payments
- db_inventory (schema inventory): warehouses, stock_entries, goods_receipts,
  gr_line_items, stock_movements
- db_vendor (schema vendor): vendors, vendor_contacts, rfqs, rfq_invitations,
  vendor_quotes, vendor_scores
- db_notification (schema notification): notifications, notification_templates
- db_camunda (schema camunda): managed by Camunda auto-schema
- db_audit (schema audit): audit_logs (append-only)

## Cross-service data rules
- No DB-level FK across services
- Validate cross-service relations via API or events
