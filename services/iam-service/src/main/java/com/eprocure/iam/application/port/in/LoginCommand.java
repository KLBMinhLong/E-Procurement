package com.eprocure.iam.application.port.in;

import java.util.UUID;

public record LoginCommand(String username, String password, UUID idempotencyKey, ClientContext clientContext) {
}
