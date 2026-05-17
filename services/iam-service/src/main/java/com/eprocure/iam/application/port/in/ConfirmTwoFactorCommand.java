package com.eprocure.iam.application.port.in;

import java.util.UUID;

public record ConfirmTwoFactorCommand(UUID userId, String code) {
}
