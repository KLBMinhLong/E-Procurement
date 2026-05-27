package com.eprocure.pr.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record MarkPurchaseRequestPendingApprovalCommand(
        UUID purchaseRequestId,
        UUID approvalProcessId,
        String camundaProcessInstanceId) {

    public MarkPurchaseRequestPendingApprovalCommand {
        purchaseRequestId = Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
        approvalProcessId = Objects.requireNonNull(approvalProcessId, "approvalProcessId must not be null");
        if (camundaProcessInstanceId == null || camundaProcessInstanceId.isBlank()) {
            throw new IllegalArgumentException("camundaProcessInstanceId must not be blank");
        }
        camundaProcessInstanceId = camundaProcessInstanceId.trim();
    }
}
