package com.eprocure.approval.application.port.in;

import java.util.UUID;

public record DeactivateApprovalRuleCommand(
        UUID id,
        UUID actorId,
        String reason) {
}
