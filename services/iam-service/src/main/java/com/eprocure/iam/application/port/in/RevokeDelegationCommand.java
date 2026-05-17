package com.eprocure.iam.application.port.in;

import java.util.UUID;

public record RevokeDelegationCommand(UUID actorId, UUID delegationId, String reason) {
}
