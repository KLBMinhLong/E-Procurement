package com.eprocure.admin.presentation.response;

import java.util.List;
import java.util.UUID;

public record AuditActorResponse(
        UUID id,
        String name,
        List<String> roles,
        String ip) {
}
