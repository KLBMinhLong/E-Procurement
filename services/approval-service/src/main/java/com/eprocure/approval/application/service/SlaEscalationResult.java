package com.eprocure.approval.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SlaEscalationResult(
        int scannedProcesses,
        int escalatedSteps,
        List<EscalatedStepView> steps) {

    public SlaEscalationResult {
        steps = List.copyOf(steps);
    }

    public record EscalatedStepView(
            UUID processId,
            UUID stepId,
            UUID previousApproverId,
            UUID escalatedToApproverId,
            Instant slaDeadline) {
    }
}
