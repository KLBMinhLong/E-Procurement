package com.eprocure.approval.application.service;

public record ApprovalRuleMutationResult(
        ApprovalRuleAdminView view,
        boolean replayed) {
}
