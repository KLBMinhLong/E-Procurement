package com.eprocure.admin.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.eprocure.admin.application.port.in.InvalidateSessionCommand;
import com.eprocure.admin.application.port.out.AdminAuditLogWriterPort;
import com.eprocure.admin.application.port.out.IamSessionAdminPort;
import com.eprocure.admin.application.service.AdminAuditContext;
import com.eprocure.admin.application.service.IdempotencyGuard;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvalidateSessionUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final String IDEMPOTENCY_KEY = "11111111-1111-4111-8111-111111111111";
    private static final UUID IDEMPOTENCY_UUID = UUID.fromString(IDEMPOTENCY_KEY);

    @Mock
    private IamSessionAdminPort iamSessionAdminPort;

    @Mock
    private AdminAuditLogWriterPort auditLogWriter;

    private InvalidateSessionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new InvalidateSessionUseCase(iamSessionAdminPort, auditLogWriter, new IdempotencyGuard());
    }

    @Test
    @DisplayName("Forward invalidate session sang IAM khi command hợp lệ")
    void should_delegate_to_iam_port_when_command_is_valid() {
        // When
        useCase.execute(command("Suspicious activity"), IDEMPOTENCY_KEY);

        // Then
        verify(iamSessionAdminPort).invalidateSession(
                SESSION_ID,
                ACTOR_ID,
                "Suspicious activity",
                IDEMPOTENCY_UUID);
        verify(auditLogWriter).recordSessionInvalidation(SESSION_ID, auditContext());
    }

    @Test
    @DisplayName("Ném VAL_001 khi reason trống")
    void should_throw_when_reason_is_blank() {
        assertThatThrownBy(() -> useCase.execute(command(" "), IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getErrorCode())
                        .isEqualTo(ErrorCode.VAL_001.code()));
        verifyNoInteractions(iamSessionAdminPort, auditLogWriter);
    }

    private InvalidateSessionCommand command(String reason) {
        return new InvalidateSessionCommand(SESSION_ID, ACTOR_ID, reason, auditContext());
    }

    private AdminAuditContext auditContext() {
        return new AdminAuditContext(
                ACTOR_ID,
                "Admin User",
                List.of("SYSTEM_CONFIG"),
                Optional.of("127.0.0.1"),
                Optional.of("PATCH"),
                Optional.of("/api/v1/admin/sessions/" + SESSION_ID + "/invalidate"),
                Optional.of("req-session-invalidate"));
    }
}
