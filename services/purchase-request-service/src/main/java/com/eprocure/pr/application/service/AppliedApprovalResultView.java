package com.eprocure.pr.application.service;

import com.eprocure.pr.domain.model.PrStatus;
import java.util.UUID;

public record AppliedApprovalResultView(
        UUID id,
        String prNumber,
        PrStatus status,
        UUID approvalProcessId) {
}
