package com.eprocure.admin.presentation.response;

import java.util.UUID;

public record ServiceConfigUpdateResponse(
        UUID actionId,
        String status,
        int updatedCount,
        boolean requiresRestart,
        String affectedService,
        boolean applied) {
}
