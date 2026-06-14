package com.eprocure.approval.domain.model;

import java.util.Objects;

public record ApprovalStepTemplate(
        int stepIndex,
        String requiredPermission,
        ApprovalStepType stepType,
        int slaHours,
        boolean required) {

    public ApprovalStepTemplate {
        if (stepIndex < 1) {
            throw new IllegalArgumentException("stepIndex must be positive");
        }
        if (requiredPermission == null || requiredPermission.isBlank()) {
            throw new IllegalArgumentException("requiredPermission must not be blank");
        }
        Objects.requireNonNull(stepType, "stepType must not be null");
        if (slaHours < 1) {
            throw new IllegalArgumentException("slaHours must be positive");
        }
        requiredPermission = requiredPermission.trim().toUpperCase();
    }
}
