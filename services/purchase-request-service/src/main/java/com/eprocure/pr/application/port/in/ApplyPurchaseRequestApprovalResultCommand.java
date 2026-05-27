package com.eprocure.pr.application.port.in;

import com.eprocure.pr.domain.model.PrStatus;
import java.util.Objects;
import java.util.UUID;

public record ApplyPurchaseRequestApprovalResultCommand(
        UUID purchaseRequestId,
        UUID approvalProcessId,
        PrStatus targetStatus,
        String comment) {

    public ApplyPurchaseRequestApprovalResultCommand {
        purchaseRequestId = Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
        approvalProcessId = Objects.requireNonNull(approvalProcessId, "approvalProcessId must not be null");
        targetStatus = Objects.requireNonNull(targetStatus, "targetStatus must not be null");
        if (targetStatus != PrStatus.APPROVED
                && targetStatus != PrStatus.REJECTED
                && targetStatus != PrStatus.CHANGES_REQUESTED) {
            throw new IllegalArgumentException("targetStatus must be an approval result status");
        }
        comment = comment == null || comment.isBlank() ? null : comment.trim();
    }
}
