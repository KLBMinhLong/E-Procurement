package com.eprocure.admin.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.eprocure.admin.application.port.in.ListActiveSessionsQuery;
import com.eprocure.admin.application.port.out.IamSessionAdminPort;
import com.eprocure.admin.application.service.ActiveSessionView;
import com.eprocure.admin.application.service.PageMeta;
import com.eprocure.admin.application.service.PageResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListActiveSessionsUseCaseTest {
    private static final UUID USER_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-06-15T00:00:00Z");

    @Mock
    private IamSessionAdminPort iamSessionAdminPort;

    @InjectMocks
    private ListActiveSessionsUseCase useCase;

    @Test
    @DisplayName("Gọi IAM session admin port khi list active sessions")
    void should_delegate_to_iam_port_when_listing_active_sessions() {
        // Given
        var page = new PageResult<>(
                List.of(session()),
                PageMeta.of(1, 1, 50, "issuedAt,desc"));
        given(iamSessionAdminPort.listActiveSessions(USER_ID, 1, 50)).willReturn(page);

        // When
        var result = useCase.execute(new ListActiveSessionsQuery(1, 50, USER_ID));

        // Then
        assertThat(result.items()).hasSize(1);
        verify(iamSessionAdminPort).listActiveSessions(USER_ID, 1, 50);
    }

    private ActiveSessionView session() {
        return new ActiveSessionView(
                SESSION_ID,
                USER_ID,
                "superadmin",
                "Super Admin",
                "127.0.0.1",
                "Mozilla",
                NOW,
                NOW,
                NOW.plusSeconds(3600));
    }
}
