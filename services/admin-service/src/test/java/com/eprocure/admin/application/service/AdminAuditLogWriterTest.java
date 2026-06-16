package com.eprocure.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.eprocure.admin.domain.model.AdminConfigAction;
import com.eprocure.admin.domain.model.AuditLogEntry;
import com.eprocure.admin.domain.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogWriterTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID ACTION_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private AuditLogRepository auditLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_write_sanitized_audit_entry_when_recording_config_update_action() throws Exception {
        AdminConfigAction action = AdminConfigAction.updateConfig(
                ACTION_ID,
                "iam-service",
                "Rotate database password secret=abc",
                2,
                true,
                IDEMPOTENCY_KEY,
                NOW,
                ACTOR_ID);

        new AdminAuditLogWriter(auditLogRepository, objectMapper, CLOCK).recordConfigAction(action, auditContext());

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).append(captor.capture());
        AuditLogEntry entry = captor.getValue();

        assertThat(entry.action()).isEqualTo("CONFIG.UPDATE_REQUESTED");
        assertThat(entry.entityType()).isEqualTo("ADMIN_CONFIG_ACTION");
        assertThat(entry.entityId()).contains(ACTION_ID);
        assertThat(entry.entityNumber()).contains("iam-service");
        assertThat(entry.occurredAt()).isEqualTo(NOW);
        assertThat(entry.actor().id()).isEqualTo(ACTOR_ID);
        assertThat(entry.actor().name()).isEqualTo("Admin User");
        assertThat(entry.actor().roles()).containsExactly("ADMIN_CONFIG_MANAGE");
        assertThat(entry.httpMethod()).contains("PUT");
        assertThat(entry.endpoint()).contains("/api/v1/admin/config/services/iam-service");
        assertThat(entry.requestId()).contains("req-admin-config");
        assertThat(entry.serviceName()).isEqualTo("admin-service");
        assertThat(entry.oldValueJson()).isEmpty();

        String payloadText = entry.newValueJson().orElseThrow();
        assertThat(payloadText)
                .doesNotContain("password")
                .doesNotContain("secret=abc")
                .doesNotContain("Rotate database");
        JsonNode payload = objectMapper.readTree(payloadText);
        assertThat(payload.get("actionId").asText()).isEqualTo(ACTION_ID.toString());
        assertThat(payload.get("actionType").asText()).isEqualTo("UPDATE_CONFIG");
        assertThat(payload.get("status").asText()).isEqualTo("PENDING_MANUAL_APPLY");
        assertThat(payload.get("serviceName").asText()).isEqualTo("iam-service");
        assertThat(payload.get("variableCount").asInt()).isEqualTo(2);
        assertThat(payload.get("requiresRestart").asBoolean()).isTrue();
        assertThat(payload.get("applied").asBoolean()).isFalse();
    }

    @Test
    void should_use_key_version_as_entity_number_when_recording_encryption_rotation_action() {
        AdminConfigAction action = AdminConfigAction.rotateEncryptionKey(
                ACTION_ID,
                "pending-v20260615000000",
                IDEMPOTENCY_KEY,
                NOW,
                ACTOR_ID);

        new AdminAuditLogWriter(auditLogRepository, objectMapper, CLOCK).recordConfigAction(action, auditContext());

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).append(captor.capture());
        AuditLogEntry entry = captor.getValue();

        assertThat(entry.action()).isEqualTo("ENCRYPTION.KEY_ROTATE_REQUESTED");
        assertThat(entry.entityNumber()).contains("pending-v20260615000000");
        assertThat(entry.description()).contains("Admin encryption key rotation request recorded");
    }

    @Test
    void should_write_sanitized_audit_entry_when_recording_session_invalidation_request() throws Exception {
        UUID sessionId = UUID.fromString("40000000-0000-4000-8000-000000000001");

        new AdminAuditLogWriter(auditLogRepository, objectMapper, CLOCK)
                .recordSessionInvalidation(sessionId, auditContext());

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).append(captor.capture());
        AuditLogEntry entry = captor.getValue();

        assertThat(entry.action()).isEqualTo("SESSION.INVALIDATE_REQUESTED");
        assertThat(entry.entityType()).isEqualTo("IAM_SESSION");
        assertThat(entry.entityId()).contains(sessionId);
        assertThat(entry.entityNumber()).contains(sessionId.toString());
        assertThat(entry.occurredAt()).isEqualTo(NOW);
        assertThat(entry.description()).contains("Admin session invalidation request recorded");

        String payloadText = entry.newValueJson().orElseThrow();
        assertThat(payloadText)
                .doesNotContain("reason")
                .doesNotContain("Suspicious activity");
        JsonNode payload = objectMapper.readTree(payloadText);
        assertThat(payload.get("actionType").asText()).isEqualTo("INVALIDATE_SESSION");
        assertThat(payload.get("sessionId").asText()).isEqualTo(sessionId.toString());
        assertThat(payload.get("invalidated").asBoolean()).isTrue();
    }

    private AdminAuditContext auditContext() {
        return new AdminAuditContext(
                ACTOR_ID,
                "Admin User",
                List.of("ADMIN_CONFIG_MANAGE"),
                Optional.of("127.0.0.1"),
                Optional.of("PUT"),
                Optional.of("/api/v1/admin/config/services/iam-service"),
                Optional.of("req-admin-config"));
    }
}
