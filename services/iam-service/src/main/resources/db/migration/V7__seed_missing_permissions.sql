-- V7__seed_missing_permissions.sql
-- Bổ sung toàn bộ permission codes còn thiếu theo DOMAIN_MODEL.md
-- và các @PreAuthorize đang dùng trong code thực tế.
--
-- Permissions đã có (V1 + V2):
--   IAM_PROFILE_READ, IAM_SESSION_REVOKE
--   ADMIN_USER_VIEW, ADMIN_USER_MANAGE, ADMIN_ROLE_MANAGE, ADMIN_DEPARTMENT_MANAGE
--   PR_CREATE, PR_VIEW_OWN, PR_APPROVE_L1, PR_APPROVE_L2
--   DELEGATION_MANAGE
--   BUDGET_VIEW_ALL, PO_ISSUE, INVOICE_MATCH
--   ORG_VIEW, ORG_APPROVER_RESOLVE

CREATE SCHEMA IF NOT EXISTS iam;

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. INSERT MISSING PERMISSIONS
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO iam.permissions (id, code, name, description, service, created_by)
VALUES
    -- ── PR (Purchase Request) ──────────────────────────────────────────────
    ('20000000-0000-0000-0000-000000000101', 'PR_VIEW_DEPARTMENT',
     'View department purchase requests',
     'View all purchase requests within the same department.',
     'PR_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000102', 'PR_VIEW_ALL',
     'View all purchase requests',
     'View all purchase requests across all departments.',
     'PR_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000103', 'PR_EDIT_OWN_DRAFT',
     'Edit own draft purchase request',
     'Edit purchase requests in DRAFT status owned by the current user.',
     'PR_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000104', 'PR_CANCEL_OWN',
     'Cancel own purchase request',
     'Cancel own purchase requests that are in DRAFT, SUBMITTED or CHANGES_REQUESTED status.',
     'PR_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000105', 'PR_APPROVE_L3',
     'Approve level 3 (C-Level)',
     'Approve high-value purchase requests at C-Level (CEO/BOD step).',
     'APPROVAL_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000106', 'PR_APPROVE_FINANCE',
     'Approve purchase request — Finance step',
     'Perform the Finance sign-off step in the approval workflow.',
     'APPROVAL_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000107', 'PR_APPROVE_EMERGENCY',
     'Approve emergency purchase request',
     'Approve emergency-priority purchase requests (compressed SLA).',
     'APPROVAL_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000108', 'PR_REQUEST_CHANGES',
     'Request changes on purchase request',
     'Send a purchase request back to the requester for revision.',
     'APPROVAL_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000109', 'PR_FORWARD',
     'Forward purchase request',
     'Forward a purchase request task to another approver for review.',
     'APPROVAL_SERVICE', '00000000-0000-0000-0000-000000000000'),

    -- ── PO (Purchase Order) ────────────────────────────────────────────────
    ('20000000-0000-0000-0000-000000000201', 'PO_CREATE',
     'Create purchase order',
     'Create a purchase order from an approved purchase request.',
     'PR_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000202', 'PO_VIEW_OWN',
     'View own purchase orders',
     'View purchase orders created by the current user.',
     'PR_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000203', 'PO_VIEW_ALL',
     'View all purchase orders',
     'View all purchase orders across all departments.',
     'PR_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000204', 'PO_EDIT',
     'Edit purchase order',
     'Edit a purchase order before it is sent to the vendor.',
     'PR_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000205', 'PO_SEND_TO_VENDOR',
     'Send purchase order to vendor',
     'Transmit an approved purchase order to the vendor.',
     'PR_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000206', 'PO_CANCEL',
     'Cancel purchase order',
     'Cancel a purchase order (soft-cancel).',
     'PR_SERVICE', '00000000-0000-0000-0000-000000000000'),

    -- ── RFQ (Request for Quotation) ────────────────────────────────────────
    ('20000000-0000-0000-0000-000000000301', 'RFQ_CREATE',
     'Create RFQ',
     'Create a request for quotation linked to a purchase request.',
     'PR_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000302', 'RFQ_VIEW',
     'View RFQ',
     'View request for quotation records.',
     'PR_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000303', 'RFQ_EVALUATE',
     'Evaluate RFQ quotes',
     'Evaluate vendor quotes and select the preferred vendor.',
     'PR_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000304', 'RFQ_AWARD',
     'Award RFQ',
     'Officially award a vendor as the winner of an RFQ.',
     'PR_SERVICE', '00000000-0000-0000-0000-000000000000'),

    -- ── GR (Goods Receipt) ─────────────────────────────────────────────────
    ('20000000-0000-0000-0000-000000000401', 'GR_CREATE',
     'Create goods receipt',
     'Record goods received against a purchase order.',
     'INVENTORY_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000402', 'GR_VIEW',
     'View goods receipts',
     'View goods receipt records.',
     'INVENTORY_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000403', 'GR_ISSUE_OUT',
     'Issue goods out of warehouse',
     'Issue inventory items out of the warehouse to end users.',
     'INVENTORY_SERVICE', '00000000-0000-0000-0000-000000000000'),

    -- ── INVOICE ────────────────────────────────────────────────────────────
    ('20000000-0000-0000-0000-000000000501', 'INVOICE_CREATE',
     'Create invoice',
     'Record a vendor invoice against a purchase order.',
     'FINANCE_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000502', 'INVOICE_VIEW',
     'View invoices',
     'View vendor invoices.',
     'FINANCE_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000503', 'INVOICE_APPROVE',
     'Approve invoice',
     'Approve a vendor invoice after 3-way matching.',
     'FINANCE_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000504', 'PAYMENT_CONFIRM',
     'Confirm payment',
     'Confirm that a vendor invoice has been paid.',
     'FINANCE_SERVICE', '00000000-0000-0000-0000-000000000000'),

    -- ── BUDGET ─────────────────────────────────────────────────────────────
    ('20000000-0000-0000-0000-000000000601', 'BUDGET_VIEW_OWN_DEPT',
     'View own department budget',
     'View the budget for the current user''s department.',
     'FINANCE_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000602', 'BUDGET_OVERRIDE',
     'Override budget',
     'Approve a purchase request that exceeds the allocated budget.',
     'FINANCE_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000603', 'BUDGET_TRANSFER_APPROVE',
     'Approve budget transfer',
     'Approve a budget transfer between departments or GL accounts.',
     'FINANCE_SERVICE', '00000000-0000-0000-0000-000000000000'),

    -- ── VENDOR ─────────────────────────────────────────────────────────────
    ('20000000-0000-0000-0000-000000000701', 'VENDOR_CREATE',
     'Create vendor',
     'Add a new vendor to the system.',
     'VENDOR_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000702', 'VENDOR_VIEW',
     'View vendors',
     'View the vendor list and vendor details.',
     'VENDOR_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000703', 'VENDOR_EDIT',
     'Edit vendor',
     'Edit vendor information.',
     'VENDOR_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000704', 'VENDOR_APPROVE',
     'Approve vendor',
     'Approve a new vendor for the Approved Vendor List (AVL).',
     'VENDOR_SERVICE', '00000000-0000-0000-0000-000000000000'),

    -- ── REPORT ─────────────────────────────────────────────────────────────
    ('20000000-0000-0000-0000-000000000801', 'REPORT_VIEW',
     'View reports',
     'Access and view procurement and finance reports.',
     'IAM_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000802', 'REPORT_EXPORT',
     'Export reports',
     'Export reports to PDF or Excel format.',
     'IAM_SERVICE', '00000000-0000-0000-0000-000000000000'),

    -- ── ADMIN (extended) ───────────────────────────────────────────────────
    ('20000000-0000-0000-0000-000000000901', 'ADMIN_APPROVAL_RULE',
     'Manage approval rules',
     'Configure approval matrix rules and routing conditions.',
     'IAM_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000902', 'ADMIN_CATALOG_MANAGE',
     'Manage item catalog',
     'Manage the catalog of purchasable items and categories.',
     'IAM_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000903', 'ADMIN_DELEGATION_MANAGE',
     'Manage all delegations (admin)',
     'View and revoke any approval delegation across the organization.',
     'IAM_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000904', 'SYSTEM_CONFIG',
     'System configuration',
     'Access and modify system-wide configuration settings.',
     'IAM_SERVICE', '00000000-0000-0000-0000-000000000000'),

    ('20000000-0000-0000-0000-000000000905', 'SYSTEM_AUDIT_VIEW',
     'View audit log',
     'View the system-wide audit trail.',
     'IAM_SERVICE', '00000000-0000-0000-0000-000000000000')

ON CONFLICT (id) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. ASSIGN PERMISSIONS TO ROLES
--
-- Role matrix (cumulative — lower roles inherit basic perms):
--
-- REQUESTER  : PR ops (create, view own, edit draft, cancel) + basic PO/vendor view
-- MANAGER    : + PR dept view, approve L1, request changes, forward + reports
-- DIRECTOR   : + PR view all, approve L2 + emergency + vendor approve + budget override
-- FINANCE    : + invoice/payment/budget full access + PO issue (already seeded)
-- ADMIN      : all admin + system perms
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO iam.role_permissions (id, role_id, permission_id, created_by)
SELECT gen_random_uuid(), r.id, p.id, '00000000-0000-0000-0000-000000000000'::UUID
FROM (
    VALUES
        -- ── REQUESTER ─────────────────────────────────────────────────────
        ('REQUESTER', 'PR_EDIT_OWN_DRAFT'),
        ('REQUESTER', 'PR_CANCEL_OWN'),
        ('REQUESTER', 'PO_VIEW_OWN'),
        ('REQUESTER', 'VENDOR_VIEW'),
        ('REQUESTER', 'REPORT_VIEW'),

        -- ── MANAGER ───────────────────────────────────────────────────────
        ('MANAGER', 'PR_VIEW_OWN'),
        ('MANAGER', 'PR_VIEW_DEPARTMENT'),
        ('MANAGER', 'PR_APPROVE_FINANCE'),
        ('MANAGER', 'PR_APPROVE_EMERGENCY'),
        ('MANAGER', 'PR_REQUEST_CHANGES'),
        ('MANAGER', 'PR_FORWARD'),
        ('MANAGER', 'PO_VIEW_OWN'),
        ('MANAGER', 'PO_VIEW_ALL'),
        ('MANAGER', 'RFQ_VIEW'),
        ('MANAGER', 'GR_VIEW'),
        ('MANAGER', 'INVOICE_VIEW'),
        ('MANAGER', 'BUDGET_VIEW_OWN_DEPT'),
        ('MANAGER', 'VENDOR_VIEW'),
        ('MANAGER', 'REPORT_VIEW'),

        -- ── DIRECTOR ──────────────────────────────────────────────────────
        ('DIRECTOR', 'PR_VIEW_OWN'),
        ('DIRECTOR', 'PR_VIEW_DEPARTMENT'),
        ('DIRECTOR', 'PR_VIEW_ALL'),
        ('DIRECTOR', 'PR_APPROVE_L3'),
        ('DIRECTOR', 'PR_APPROVE_FINANCE'),
        ('DIRECTOR', 'PR_APPROVE_EMERGENCY'),
        ('DIRECTOR', 'PR_REQUEST_CHANGES'),
        ('DIRECTOR', 'PR_FORWARD'),
        ('DIRECTOR', 'PO_VIEW_OWN'),
        ('DIRECTOR', 'PO_VIEW_ALL'),
        ('DIRECTOR', 'PO_CREATE'),
        ('DIRECTOR', 'PO_SEND_TO_VENDOR'),
        ('DIRECTOR', 'RFQ_VIEW'),
        ('DIRECTOR', 'RFQ_EVALUATE'),
        ('DIRECTOR', 'RFQ_AWARD'),
        ('DIRECTOR', 'GR_VIEW'),
        ('DIRECTOR', 'INVOICE_VIEW'),
        ('DIRECTOR', 'BUDGET_VIEW_OWN_DEPT'),
        ('DIRECTOR', 'BUDGET_VIEW_ALL'),
        ('DIRECTOR', 'BUDGET_OVERRIDE'),
        ('DIRECTOR', 'BUDGET_TRANSFER_APPROVE'),
        ('DIRECTOR', 'VENDOR_VIEW'),
        ('DIRECTOR', 'VENDOR_APPROVE'),
        ('DIRECTOR', 'REPORT_VIEW'),
        ('DIRECTOR', 'REPORT_EXPORT'),

        -- ── FINANCE ───────────────────────────────────────────────────────
        ('FINANCE', 'PR_VIEW_ALL'),
        ('FINANCE', 'PR_APPROVE_FINANCE'),
        ('FINANCE', 'PO_VIEW_OWN'),
        ('FINANCE', 'PO_VIEW_ALL'),
        ('FINANCE', 'PO_EDIT'),
        ('FINANCE', 'PO_CANCEL'),
        ('FINANCE', 'RFQ_VIEW'),
        ('FINANCE', 'GR_VIEW'),
        ('FINANCE', 'INVOICE_CREATE'),
        ('FINANCE', 'INVOICE_VIEW'),
        ('FINANCE', 'INVOICE_APPROVE'),
        ('FINANCE', 'PAYMENT_CONFIRM'),
        ('FINANCE', 'BUDGET_VIEW_OWN_DEPT'),
        ('FINANCE', 'BUDGET_OVERRIDE'),
        ('FINANCE', 'BUDGET_TRANSFER_APPROVE'),
        ('FINANCE', 'VENDOR_VIEW'),
        ('FINANCE', 'REPORT_VIEW'),
        ('FINANCE', 'REPORT_EXPORT'),

        -- ── ADMIN ─────────────────────────────────────────────────────────
        ('ADMIN', 'PR_VIEW_ALL'),
        ('ADMIN', 'PO_VIEW_ALL'),
        ('ADMIN', 'RFQ_VIEW'),
        ('ADMIN', 'GR_VIEW'),
        ('ADMIN', 'INVOICE_VIEW'),
        ('ADMIN', 'BUDGET_VIEW_ALL'),
        ('ADMIN', 'VENDOR_VIEW'),
        ('ADMIN', 'REPORT_VIEW'),
        ('ADMIN', 'REPORT_EXPORT'),
        ('ADMIN', 'ADMIN_APPROVAL_RULE'),
        ('ADMIN', 'ADMIN_CATALOG_MANAGE'),
        ('ADMIN', 'ADMIN_DELEGATION_MANAGE'),
        ('ADMIN', 'SYSTEM_CONFIG'),
        ('ADMIN', 'SYSTEM_AUDIT_VIEW'),
        ('ADMIN', 'ORG_VIEW'),
        ('ADMIN', 'ORG_APPROVER_RESOLVE')

) AS seed(role_code, permission_code)
JOIN iam.roles r
    ON r.is_deleted = FALSE AND UPPER(r.code) = seed.role_code
JOIN iam.permissions p
    ON p.is_deleted = FALSE AND UPPER(p.code) = seed.permission_code
ON CONFLICT (role_id, permission_id) DO UPDATE
    SET is_deleted  = FALSE,
        deleted_at  = NULL,
        deleted_by  = NULL,
        updated_at  = NOW(),
        updated_by  = '00000000-0000-0000-0000-000000000000'::UUID;

COMMENT ON TABLE iam.permissions IS 'Permission codes consumed by @PreAuthorize. Seeded through V1, V2, V7.';
