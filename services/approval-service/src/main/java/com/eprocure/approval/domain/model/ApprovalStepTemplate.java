package com.eprocure.approval.domain.model;

import java.util.Objects;

public record ApprovalStepTemplate(
        int stepIndex,
        String approverRole,
        ApprovalStepType stepType,
        int slaHours,
        boolean required) {

    public ApprovalStepTemplate {
        if (stepIndex < 1) {
            throw new IllegalArgumentException("stepIndex must be positive");
        }
        if (approverRole == null || approverRole.isBlank()) {
            throw new IllegalArgumentException("approverRole must not be blank");
        }
        Objects.requireNonNull(stepType, "stepType must not be null");
        if (slaHours < 1) {
            throw new IllegalArgumentException("slaHours must be positive");
        }
        approverRole = approverRole.trim().toUpperCase();
    }
}
