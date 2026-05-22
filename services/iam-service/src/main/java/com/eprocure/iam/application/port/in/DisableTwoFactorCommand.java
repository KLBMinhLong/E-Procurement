package com.eprocure.iam.application.port.in;

import java.util.UUID;

public record DisableTwoFactorCommand(UUID userId) {
}
