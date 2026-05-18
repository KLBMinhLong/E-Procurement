package com.eprocure.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eprocure.iam.application.port.in.AssignUserRolesCommand;
import com.eprocure.iam.application.port.out.SessionCachePort;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.OpaqueTokenService;
import com.eprocure.iam.application.service.SessionData;
import com.eprocure.iam.application.service.SessionService;
import com.eprocure.iam.domain.model.SessionRecord;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.model.UserStatus;
import com.eprocure.iam.domain.repository.RoleRepository;
import com.eprocure.iam.domain.repository.SessionRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import com.eprocure.iam.testsupport.StubPermissionResolutionService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssignUserRolesUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID DEPARTMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void should_evict_active_session_cache_when_user_roles_change() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        FakeSessionCachePort sessionCachePort = new FakeSessionCachePort();
        SessionService sessionService = new SessionService(
                new FakeSessionRepository(List.of("cached-token-hash")),
                userRepository,
                sessionCachePort,
                new OpaqueTokenService(),
                new StubPermissionResolutionService(),
                8);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(roleRepository.findExistingCodes(Set.of("APPROVER"))).thenReturn(Set.of("APPROVER"));
        AssignUserRolesUseCase useCase = new AssignUserRolesUseCase(
                userRepository,
                roleRepository,
                new IdempotencyGuard(),
                sessionService);

        useCase.execute(
                new AssignUserRolesCommand(ACTOR_ID, USER_ID, java.util.List.of("approver")),
                UUID.randomUUID().toString());

        verify(userRepository).replaceRoles(USER_ID, Set.of("APPROVER"), ACTOR_ID);
        assertThat(sessionCachePort.evictedTokenHashes).containsExactly("cached-token-hash");
    }

    private static User activeUser() {
        return User.create(
                USER_ID,
                "EMP-2025-00002",
                "approver",
                "approver@eprocure.local",
                "Approver User",
                DEPARTMENT_ID,
                UserStatus.ACTIVE,
                Instant.parse("2026-05-17T00:00:00Z"));
    }

    private static final class FakeSessionRepository implements SessionRepository {
        private final List<String> activeTokenHashes;

        private FakeSessionRepository(List<String> activeTokenHashes) {
            this.activeTokenHashes = activeTokenHashes;
        }

        @Override
        public void save(SessionRecord sessionRecord) {
        }

        @Override
        public void revokeActiveByUserId(UUID userId, UUID revokedBy, Instant revokedAt) {
        }

        @Override
        public void revokeByTokenHash(String tokenHash, UUID revokedBy, Instant revokedAt) {
        }

        @Override
        public Optional<SessionRecord> findActiveByTokenHash(String tokenHash, Instant now) {
            return Optional.empty();
        }

        @Override
        public List<String> findActiveTokenHashesByUserId(UUID userId, Instant now) {
            return activeTokenHashes;
        }
    }

    private static final class FakeSessionCachePort implements SessionCachePort {
        private final List<String> evictedTokenHashes = new ArrayList<>();

        @Override
        public void store(SessionData sessionData, Duration ttl) {
        }

        @Override
        public Optional<SessionData> findByTokenHash(String tokenHash) {
            return Optional.empty();
        }

        @Override
        public void evict(String tokenHash) {
            evictedTokenHashes.add(tokenHash);
        }
    }
}
