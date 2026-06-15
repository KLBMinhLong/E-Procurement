package com.eprocure.admin.application.service;

import com.eprocure.admin.domain.model.AdminConfigActionStatus;
import java.util.UUID;

public record ServiceConfigUpdateResult(
        UUID actionId,
        AdminConfigActionStatus status,
        int updatedCount,
        boolean requiresRestart,
        String affectedService,
        boolean applied,
        boolean replayed) {
}
