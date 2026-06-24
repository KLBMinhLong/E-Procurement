package com.eprocure.admin.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record AdminConfigAction(
        UUID id,
        AdminConfigActionType actionType,
        AdminConfigActionStatus status,
        Optional<String> serviceName,
        Optional<String> changeReason,
        int variableCount,
        boolean requiresRestart,
        Optional<Integer> estimatedDowntimeSeconds,
        Optional<String> keyVersion,
        UUID idempotencyKey,
        Instant requestedAt,
        UUID createdBy) {

    public AdminConfigAction {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(actionType, "actionType must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (variableCount < 0) {
            throw new IllegalArgumentException("variableCount must not be negative");
        }
        serviceName = normalize(serviceName);
        changeReason = normalize(changeReason);
        estimatedDowntimeSeconds = estimatedDowntimeSeconds == null ? Optional.empty() : estimatedDowntimeSeconds;
        keyVersion = normalize(keyVersion);
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        Objects.requireNonNull(createdBy, "createdBy must not be null");
    }

    public static AdminConfigAction updateConfig(
            UUID id,
            String serviceName,
            String changeReason,
            int variableCount,
            boolean requiresRestart,
            UUID idempotencyKey,
            Instant requestedAt,
            UUID createdBy) {
        return new AdminConfigAction(
                id,
                AdminConfigActionType.UPDATE_CONFIG,
                AdminConfigActionStatus.PENDING_MANUAL_APPLY,
                Optional.of(serviceName),
                Optional.of(changeReason),
                variableCount,
                requiresRestart,
                Optional.empty(),
                Optional.empty(),
                idempotencyKey,
                requestedAt,
                createdBy);
    }

    public static AdminConfigAction restartService(
            UUID id,
            String serviceName,
            String reason,
            int estimatedDowntimeSeconds,
            UUID idempotencyKey,
            Instant requestedAt,
            UUID createdBy) {
        return new AdminConfigAction(
                id,
                AdminConfigActionType.RESTART_SERVICE,
                AdminConfigActionStatus.PENDING_MANUAL_APPLY,
                Optional.of(serviceName),
                Optional.of(reason),
                0,
                true,
                Optional.of(estimatedDowntimeSeconds),
                Optional.empty(),
                idempotencyKey,
                requestedAt,
                createdBy);
    }

    public static AdminConfigAction rotateEncryptionKey(
            UUID id,
            String keyVersion,
            UUID idempotencyKey,
            Instant requestedAt,
            UUID createdBy) {
        return new AdminConfigAction(
                id,
                AdminConfigActionType.ROTATE_ENCRYPTION_KEY,
                AdminConfigActionStatus.PENDING_MANUAL_APPLY,
                Optional.empty(),
                Optional.empty(),
                0,
                false,
                Optional.empty(),
                Optional.of(keyVersion),
                idempotencyKey,
                requestedAt,
                createdBy);
    }

    private static Optional<String> normalize(Optional<String> value) {
        if (value == null || value.isEmpty() || value.get().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.get().trim());
    }
}
