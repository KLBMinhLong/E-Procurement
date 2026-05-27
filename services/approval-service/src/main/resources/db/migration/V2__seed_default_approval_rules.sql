-- V2__seed_default_approval_rules.sql
-- Migration: Seed baseline approval rule matrix.
-- Author: Codex
-- Date: 2026-05-27

WITH system_actor AS (
    SELECT '00000000-0000-0000-0000-000000000000'::uuid AS id
),
upsert_rules AS (
    INSERT INTO approval.approval_rules (
        rule_name,
        priority,
        is_active,
        rule_type,
        min_value,
        max_value,
        categories,
        department_ids,
        priorities,
        description,
        created_by
    )
    SELECT *
    FROM (
        VALUES
            ('EMERGENCY', 1000, TRUE, 'DEFAULT', NULL::numeric, NULL::numeric, NULL::varchar[], NULL::uuid[], ARRAY['EMERGENCY']::varchar[], 'Emergency PR uses parallel Manager and Director approval with post-audit follow-up.', (SELECT id FROM system_actor)),
            ('CAT_CAPEX', 700, TRUE, 'CATEGORY', NULL::numeric, NULL::numeric, ARRAY['CAPEX']::varchar[], NULL::uuid[], NULL::varchar[], 'CAPEX category adds Finance Director and CEO approval.', (SELECT id FROM system_actor)),
            ('CAT_IT_SOFTWARE', 650, TRUE, 'CATEGORY', NULL::numeric, NULL::numeric, ARRAY['SOFTWARE','SAAS','IT_SOFTWARE']::varchar[], NULL::uuid[], NULL::varchar[], 'Software and SaaS purchases add CISO and IT Manager approval.', (SELECT id FROM system_actor)),
            ('VALUE_OVER_500M', 600, TRUE, 'VALUE', 500000000.0000, NULL::numeric, NULL::varchar[], NULL::uuid[], ARRAY['NORMAL','URGENT']::varchar[], 'PR amount greater than or equal to 500M VND.', (SELECT id FROM system_actor)),
            ('VALUE_200M_500M', 500, TRUE, 'VALUE', 200000000.0000, 500000000.0000, NULL::varchar[], NULL::uuid[], ARRAY['NORMAL','URGENT']::varchar[], 'PR amount from 200M to under 500M VND.', (SELECT id FROM system_actor)),
            ('VALUE_50M_200M', 400, TRUE, 'VALUE', 50000000.0000, 200000000.0000, NULL::varchar[], NULL::uuid[], ARRAY['NORMAL','URGENT']::varchar[], 'PR amount from 50M to under 200M VND.', (SELECT id FROM system_actor)),
            ('VALUE_20M_50M', 300, TRUE, 'VALUE', 20000000.0000, 50000000.0000, NULL::varchar[], NULL::uuid[], ARRAY['NORMAL','URGENT']::varchar[], 'PR amount from 20M to under 50M VND.', (SELECT id FROM system_actor)),
            ('VALUE_5M_20M', 200, TRUE, 'VALUE', 5000000.0000, 20000000.0000, NULL::varchar[], NULL::uuid[], ARRAY['NORMAL','URGENT']::varchar[], 'PR amount from 5M to under 20M VND.', (SELECT id FROM system_actor)),
            ('VALUE_UNDER_5M', 100, TRUE, 'VALUE', NULL::numeric, 5000000.0000, NULL::varchar[], NULL::uuid[], ARRAY['NORMAL','URGENT']::varchar[], 'PR amount below 5M VND.', (SELECT id FROM system_actor)),
            ('DEFAULT', 10, TRUE, 'DEFAULT', NULL::numeric, NULL::numeric, NULL::varchar[], NULL::uuid[], NULL::varchar[], 'Fallback approval rule when no value-specific rule matches.', (SELECT id FROM system_actor))
    ) AS seed(rule_name, priority, is_active, rule_type, min_value, max_value, categories, department_ids, priorities, description, created_by)
    ON CONFLICT DO NOTHING
    RETURNING id, rule_name, created_by
)
INSERT INTO approval.approval_rule_steps (
    rule_id,
    step_index,
    approver_role,
    step_type,
    sla_hours,
    is_required,
    created_by
)
SELECT rules.id, steps.step_index, steps.approver_role, steps.step_type, steps.sla_hours, TRUE, rules.created_by
FROM approval.approval_rules rules
JOIN (
    VALUES
        ('EMERGENCY', 1, 'MANAGER', 'PARALLEL', 2),
        ('EMERGENCY', 1, 'DIRECTOR', 'PARALLEL', 4),
        ('EMERGENCY', 2, 'POST_AUDIT', 'SEQUENTIAL', 24),
        ('CAT_CAPEX', 1, 'FINANCE_DIRECTOR', 'SEQUENTIAL', 48),
        ('CAT_CAPEX', 2, 'CEO', 'SEQUENTIAL', 48),
        ('CAT_IT_SOFTWARE', 1, 'CISO', 'SEQUENTIAL', 48),
        ('CAT_IT_SOFTWARE', 2, 'IT_MANAGER', 'SEQUENTIAL', 48),
        ('VALUE_OVER_500M', 1, 'MANAGER', 'SEQUENTIAL', 48),
        ('VALUE_OVER_500M', 2, 'DIRECTOR', 'SEQUENTIAL', 48),
        ('VALUE_OVER_500M', 3, 'BOD', 'PARALLEL', 72),
        ('VALUE_OVER_500M', 4, 'CFO', 'SEQUENTIAL', 48),
        ('VALUE_200M_500M', 1, 'MANAGER', 'SEQUENTIAL', 48),
        ('VALUE_200M_500M', 2, 'DIRECTOR', 'SEQUENTIAL', 48),
        ('VALUE_200M_500M', 3, 'CEO', 'SEQUENTIAL', 48),
        ('VALUE_200M_500M', 4, 'CFO', 'SEQUENTIAL', 48),
        ('VALUE_50M_200M', 1, 'MANAGER', 'SEQUENTIAL', 48),
        ('VALUE_50M_200M', 2, 'DIRECTOR', 'SEQUENTIAL', 48),
        ('VALUE_50M_200M', 3, 'CFO', 'SEQUENTIAL', 48),
        ('VALUE_20M_50M', 1, 'MANAGER', 'SEQUENTIAL', 48),
        ('VALUE_20M_50M', 2, 'DIRECTOR', 'SEQUENTIAL', 48),
        ('VALUE_20M_50M', 3, 'FINANCE', 'SEQUENTIAL', 48),
        ('VALUE_5M_20M', 1, 'MANAGER', 'SEQUENTIAL', 48),
        ('VALUE_5M_20M', 2, 'FINANCE', 'SEQUENTIAL', 48),
        ('VALUE_UNDER_5M', 1, 'MANAGER', 'SEQUENTIAL', 48),
        ('DEFAULT', 1, 'MANAGER', 'SEQUENTIAL', 48)
) AS steps(rule_name, step_index, approver_role, step_type, sla_hours)
    ON steps.rule_name = rules.rule_name
WHERE rules.is_deleted = FALSE
ON CONFLICT DO NOTHING;
