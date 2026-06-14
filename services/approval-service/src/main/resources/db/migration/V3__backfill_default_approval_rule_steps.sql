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
    required_permission,
    step_type,
    sla_hours,
    is_required,
    created_by
)
SELECT rules.id, steps.step_index, steps.required_permission, steps.step_type, steps.sla_hours, TRUE, (SELECT id FROM system_actor)
FROM approval.approval_rules rules
JOIN (
    VALUES
        ('EMERGENCY', 1, 'PR_APPROVE_EMERGENCY', 'PARALLEL', 2),
        ('EMERGENCY', 1, 'PR_APPROVE_L2', 'PARALLEL', 4),
        ('EMERGENCY', 2, 'PR_APPROVE_FINANCE', 'SEQUENTIAL', 24),
        ('CAT_CAPEX', 1, 'PR_APPROVE_FINANCE', 'SEQUENTIAL', 48),
        ('CAT_CAPEX', 2, 'PR_APPROVE_L3', 'SEQUENTIAL', 48),
        ('CAT_IT_SOFTWARE', 1, 'PR_APPROVE_L3', 'SEQUENTIAL', 48),
        ('VALUE_OVER_500M', 1, 'PR_APPROVE_L1', 'SEQUENTIAL', 48),
        ('VALUE_OVER_500M', 2, 'PR_APPROVE_L2', 'SEQUENTIAL', 48),
        ('VALUE_OVER_500M', 3, 'PR_APPROVE_L3', 'SEQUENTIAL', 72),
        ('VALUE_OVER_500M', 4, 'PR_APPROVE_FINANCE', 'SEQUENTIAL', 48),
        ('VALUE_200M_500M', 1, 'PR_APPROVE_L1', 'SEQUENTIAL', 48),
        ('VALUE_200M_500M', 2, 'PR_APPROVE_L2', 'SEQUENTIAL', 48),
        ('VALUE_200M_500M', 3, 'PR_APPROVE_L3', 'SEQUENTIAL', 48),
        ('VALUE_200M_500M', 4, 'PR_APPROVE_FINANCE', 'SEQUENTIAL', 48),
        ('VALUE_50M_200M', 1, 'PR_APPROVE_L1', 'SEQUENTIAL', 48),
        ('VALUE_50M_200M', 2, 'PR_APPROVE_L2', 'SEQUENTIAL', 48),
        ('VALUE_50M_200M', 3, 'PR_APPROVE_FINANCE', 'SEQUENTIAL', 48),
        ('VALUE_20M_50M', 1, 'PR_APPROVE_L1', 'SEQUENTIAL', 48),
        ('VALUE_20M_50M', 2, 'PR_APPROVE_L2', 'SEQUENTIAL', 48),
        ('VALUE_20M_50M', 3, 'PR_APPROVE_FINANCE', 'SEQUENTIAL', 48),
        ('VALUE_5M_20M', 1, 'PR_APPROVE_L1', 'SEQUENTIAL', 48),
        ('VALUE_5M_20M', 2, 'PR_APPROVE_FINANCE', 'SEQUENTIAL', 48),
        ('VALUE_UNDER_5M', 1, 'PR_APPROVE_L1', 'SEQUENTIAL', 48),
        ('DEFAULT', 1, 'PR_APPROVE_L1', 'SEQUENTIAL', 48)
) AS steps(rule_name, step_index, required_permission, step_type, sla_hours)
    ON steps.rule_name = rules.rule_name
WHERE rules.is_deleted = FALSE
ON CONFLICT DO NOTHING;
