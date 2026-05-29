-- V3__backfill_default_approval_rule_steps.sql
-- Migration: Backfill baseline approval rule step templates for environments where V2 inserted rules first.
-- Author: Codex
-- Date: 2026-05-30

WITH system_actor AS (
    SELECT '00000000-0000-0000-0000-000000000000'::uuid AS id
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
SELECT rules.id, steps.step_index, steps.approver_role, steps.step_type, steps.sla_hours, TRUE, (SELECT id FROM system_actor)
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
