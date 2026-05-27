package com.eprocure.approval.application.port.out;

import java.util.UUID;

public interface PurchaseRequestStatusPort {
    void markPendingApproval(MarkPendingApprovalCommand command);

    record MarkPendingApprovalCommand(
            UUID purchaseRequestId,
            UUID approvalProcessId,
            String camundaProcessInstanceId) {
        public MarkPendingApprovalCommand {
            if (purchaseRequestId == null) {
                throw new IllegalArgumentException("purchaseRequestId must not be null");
            }
            if (approvalProcessId == null) {
                throw new IllegalArgumentException("approvalProcessId must not be null");
            }
            if (camundaProcessInstanceId == null || camundaProcessInstanceId.isBlank()) {
                throw new IllegalArgumentException("camundaProcessInstanceId must not be blank");
            }
            camundaProcessInstanceId = camundaProcessInstanceId.trim();
        }
    }
}
