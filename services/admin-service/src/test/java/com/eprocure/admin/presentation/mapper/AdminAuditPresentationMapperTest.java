package com.eprocure.admin.presentation.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.admin.domain.model.AuditActor;
import com.eprocure.admin.domain.model.AuditLogEntry;
import com.eprocure.admin.presentation.response.AuditLogEntryResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminAuditPresentationMapperTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");

    private final AdminAuditPresentationMapper mapper = new AdminAuditPresentationMapper(new ObjectMapper());

    @Test
    @DisplayName("Map audit entry với JSON old/new value thành response object")
    void should_map_json_values_when_audit_entry_has_change_payload() {
        // Given
        AuditLogEntry entry = new AuditLogEntry(
                10L,
                new AuditActor(ACTOR_ID, "Super Admin", List.of("SUPER_ADMIN"), "127.0.0.1"),
                "CONFIG.UPDATED",
                "SERVICE_CONFIG",
                Optional.empty(),
                Optional.of("admin-service"),
                Instant.parse("2026-06-15T00:00:00Z"),
                Optional.of("PUT"),
                Optional.of("/api/v1/admin/config/services/admin-service"),
                Optional.of("request-1"),
                true,
                Optional.empty(),
                Optional.of("{\"enabled\":false}"),
                Optional.of("{\"enabled\":true}"),
                Optional.of("Enabled admin config"),
                "admin-service");

        // When
        AuditLogEntryResponse response = mapper.toResponseList(List.of(entry)).get(0);

        // Then
        assertThat(response.actor().roles()).containsExactly("SUPER_ADMIN");
        assertThat(response.oldValue()).isInstanceOf(JsonNode.class);
        assertThat(((JsonNode) response.newValue()).get("enabled").asBoolean()).isTrue();
    }
}
