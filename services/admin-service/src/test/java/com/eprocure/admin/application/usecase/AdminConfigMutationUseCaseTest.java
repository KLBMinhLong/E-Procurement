package com.eprocure.admin.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.eprocure.admin.application.port.in.ConfigVariableChange;
import com.eprocure.admin.application.port.in.RestartServiceCommand;
import com.eprocure.admin.application.port.in.RotateEncryptionKeyCommand;
import com.eprocure.admin.application.port.in.UpdateServiceConfigCommand;
import com.eprocure.admin.application.port.out.AdminAuditLogWriterPort;
import com.eprocure.admin.application.port.out.IamSecurityConfirmationPort;
import com.eprocure.admin.application.service.AdminAuditContext;
import com.eprocure.admin.application.service.IdempotencyGuard;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import com.eprocure.admin.domain.model.AdminConfigAction;
import com.eprocure.admin.domain.model.AdminConfigActionStatus;
import com.eprocure.admin.domain.model.AdminConfigActionType;
import com.eprocure.admin.domain.model.ServiceConfig;
import com.eprocure.admin.domain.model.ServiceStatus;
import com.eprocure.admin.domain.repository.AdminConfigActionRepository;
import com.eprocure.admin.domain.repository.ServiceConfigRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminConfigMutationUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID ACTION_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private ServiceConfigRepository serviceConfigRepository;

    @Mock
    private AdminConfigActionRepository actionRepository;

    @Mock
    private AdminAuditLogWriterPort auditLogWriter;

    @Mock
    private IamSecurityConfirmationPort confirmationPort;

    private UpdateServiceConfigUseCase updateUseCase;
    private RestartServiceUseCase restartUseCase;
    private RotateEncryptionKeyUseCase rotateUseCase;

    @BeforeEach
    void setUp() {
        IdempotencyGuard idempotencyGuard = new IdempotencyGuard();
        updateUseCase = new UpdateServiceConfigUseCase(
                serviceConfigRepository,
                actionRepository,
                auditLogWriter,
                confirmationPort,
                idempotencyGuard,
                CLOCK);
        restartUseCase = new RestartServiceUseCase(
                serviceConfigRepository,
                actionRepository,
                auditLogWriter,
                confirmationPort,
                idempotencyGuard,
                CLOCK);
        rotateUseCase = new RotateEncryptionKeyUseCase(
                actionRepository,
                auditLogWriter,
                confirmationPort,
                idempotencyGuard,
                CLOCK);
    }

    @Test
    void should_record_update_config_action_when_confirmation_is_valid() {
        given(actionRepository.findByIdempotencyKey(ACTOR_ID, IDEMPOTENCY_KEY)).willReturn(Optional.empty());
        given(serviceConfigRepository.findByName("iam-service")).willReturn(Optional.of(serviceConfig("iam-service")));
        given(actionRepository.save(any(AdminConfigAction.class))).willAnswer(invocation -> invocation.getArgument(0));

        var result = updateUseCase.execute(updateCommand(), IDEMPOTENCY_KEY.toString());

        assertThat(result.updatedCount()).isEqualTo(2);
        assertThat(result.status()).isEqualTo(AdminConfigActionStatus.PENDING_MANUAL_APPLY);
        assertThat(result.applied()).isFalse();
        assertThat(result.replayed()).isFalse();
        verify(confirmationPort).verifyTotp(ACTOR_ID, "123456", IDEMPOTENCY_KEY);
        ArgumentCaptor<AdminConfigAction> captor = ArgumentCaptor.forClass(AdminConfigAction.class);
        verify(actionRepository).save(captor.capture());
        assertThat(captor.getValue().actionType()).isEqualTo(AdminConfigActionType.UPDATE_CONFIG);
        assertThat(captor.getValue().serviceName()).contains("iam-service");
        assertThat(captor.getValue().variableCount()).isEqualTo(2);
        verify(auditLogWriter).recordConfigAction(captor.getValue(), auditContext());
    }

    @Test
    void should_replay_update_config_action_when_idempotency_key_exists() {
        AdminConfigAction existing = AdminConfigAction.updateConfig(
                ACTION_ID,
                "iam-service",
                "Rotate database credential safely",
                1,
                true,
                IDEMPOTENCY_KEY,
                NOW,
                ACTOR_ID);
        given(actionRepository.findByIdempotencyKey(ACTOR_ID, IDEMPOTENCY_KEY)).willReturn(Optional.of(existing));

        var result = updateUseCase.execute(updateCommand(), IDEMPOTENCY_KEY.toString());

        assertThat(result.actionId()).isEqualTo(ACTION_ID);
        assertThat(result.replayed()).isTrue();
        assertThat(result.applied()).isFalse();
        verifyNoInteractions(confirmationPort, serviceConfigRepository, auditLogWriter);
    }

    @Test
    void should_throw_when_restart_service_config_is_not_found() {
        given(actionRepository.findByIdempotencyKey(ACTOR_ID, IDEMPOTENCY_KEY)).willReturn(Optional.empty());
        given(serviceConfigRepository.findByName("missing-service")).willReturn(Optional.empty());

        assertThatThrownBy(() -> restartUseCase.execute(
                new RestartServiceCommand(ACTOR_ID, "missing-service", "123456", "Apply patched runtime config", auditContext()),
                IDEMPOTENCY_KEY.toString()))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.SERVICE_CONFIG_NOT_FOUND.code()));
        verifyNoInteractions(confirmationPort, auditLogWriter);
    }

    @Test
    void should_record_rotate_encryption_key_action_when_confirmation_is_valid() {
        given(actionRepository.findByIdempotencyKey(ACTOR_ID, IDEMPOTENCY_KEY)).willReturn(Optional.empty());
        given(actionRepository.save(any(AdminConfigAction.class))).willAnswer(invocation -> invocation.getArgument(0));

        var result = rotateUseCase.execute(
                new RotateEncryptionKeyCommand(ACTOR_ID, "123456", 2048, auditContext()),
                IDEMPOTENCY_KEY.toString());

        assertThat(result.newKeyVersion()).isEqualTo("pending-v20260615000000");
        assertThat(result.rotatedAt()).isEqualTo(NOW);
        assertThat(result.oldKeyRetiredAt()).isEqualTo(NOW.plusSeconds(86_400));
        assertThat(result.applied()).isFalse();
        verify(confirmationPort).verifyTotp(ACTOR_ID, "123456", IDEMPOTENCY_KEY);
        ArgumentCaptor<AdminConfigAction> captor = ArgumentCaptor.forClass(AdminConfigAction.class);
        verify(actionRepository).save(captor.capture());
        assertThat(captor.getValue().actionType()).isEqualTo(AdminConfigActionType.ROTATE_ENCRYPTION_KEY);
        verify(auditLogWriter).recordConfigAction(captor.getValue(), auditContext());
    }

    @Test
    void should_throw_when_variable_keys_are_duplicated() {
        given(actionRepository.findByIdempotencyKey(ACTOR_ID, IDEMPOTENCY_KEY)).willReturn(Optional.empty());
        UpdateServiceConfigCommand command = new UpdateServiceConfigCommand(
                ACTOR_ID,
                "iam-service",
                List.of(
                        new ConfigVariableChange("REDIS_HOST", "redis", false, Optional.empty()),
                        new ConfigVariableChange("redis_host", "redis-2", false, Optional.empty())),
                "123456",
                false,
                "Update duplicated config keys",
                auditContext());

        assertThatThrownBy(() -> updateUseCase.execute(command, IDEMPOTENCY_KEY.toString()))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.VAL_001.code()));
        verifyNoInteractions(confirmationPort, serviceConfigRepository, auditLogWriter);
    }

    private UpdateServiceConfigCommand updateCommand() {
        return new UpdateServiceConfigCommand(
                ACTOR_ID,
                "iam-service",
                List.of(
                        new ConfigVariableChange("REDIS_HOST", "redis", false, Optional.empty()),
                        new ConfigVariableChange("REDIS_PORT", "6379", false, Optional.empty())),
                "123456",
                true,
                "Rotate database credential safely",
                auditContext());
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

    private ServiceConfig serviceConfig(String serviceName) {
        return new ServiceConfig(
                serviceName,
                "IAM Service",
                "http://localhost:8081",
                ServiceStatus.UNKNOWN,
                Optional.of("0.1.0-SNAPSHOT"),
                List.of(),
                Optional.empty());
    }
}
