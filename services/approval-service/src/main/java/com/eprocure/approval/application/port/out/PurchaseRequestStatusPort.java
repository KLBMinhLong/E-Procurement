package com.eprocure.approval.application.port.out;

import java.util.UUID;

public interface PurchaseRequestStatusPort {
    void markPendingApproval(MarkPendingApprovalCommand command);

    void markApproved(ApprovalResultCommand command);

    void markRejected(ApprovalResultCommand command);

    void markChangesRequested(ApprovalResultCommand command);

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

    record ApprovalResultCommand(
            UUID purchaseRequestId,
            UUID approvalProcessId,
            String idempotencyKey,
            String comment) {
        public ApprovalResultCommand {
            if (purchaseRequestId == null) {
                throw new IllegalArgumentException("purchaseRequestId must not be null");
            }
            if (approvalProcessId == null) {
                throw new IllegalArgumentException("approvalProcessId must not be null");
            }
            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                throw new IllegalArgumentException("idempotencyKey must not be blank");
            }
            idempotencyKey = idempotencyKey.trim();
            comment = comment == null || comment.isBlank() ? null : comment.trim();
        }
    }
}
