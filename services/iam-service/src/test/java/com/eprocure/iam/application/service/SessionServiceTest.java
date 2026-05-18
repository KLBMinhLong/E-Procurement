package com.eprocure.iam.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eprocure.iam.application.port.out.PermissionCachePort;
import com.eprocure.iam.application.port.out.SessionCachePort;
import com.eprocure.iam.domain.model.SessionRecord;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.model.UserStatus;
import com.eprocure.iam.domain.repository.RoleRepository;
import com.eprocure.iam.domain.repository.SessionRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionServiceTest {
    private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void should_resolve_authorities_from_role_permission_cache_not_session_snapshot() {
        OpaqueTokenService opaqueTokenService = new OpaqueTokenService();
        String rawToken = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        String tokenHash = opaqueTokenService.hash(rawToken);
        FakeSessionCachePort sessionCachePort = new FakeSessionCachePort();
        sessionCachePort.store(
                new SessionData(tokenHash, USER_ID, Instant.now().plus(Duration.ofHours(1)), Set.of("REQUESTER")),
                Duration.ofHours(1));

        FakePermissionCachePort permissionCachePort = new FakePermissionCachePort();
        PermissionResolutionService permissionResolutionService = new PermissionResolutionService(
                mock(UserRepository.class),
                mock(RoleRepository.class),
                permissionCachePort,
                15);
        permissionResolutionService.refreshRolePermissions("REQUESTER", Set.of("IAM_PROFILE_READ"));

        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        SessionService sessionService = new SessionService(
                mock(SessionRepository.class),
                userRepository,
                sessionCachePort,
                opaqueTokenService,
                permissionResolutionService,
                8);

        assertThat(sessionService.authenticate(rawToken).orElseThrow().getPermissions())
                .containsExactly("IAM_PROFILE_READ");

        permissionResolutionService.refreshRolePermissions("REQUESTER", Set.of("ADMIN_ROLE_MANAGE"));

        assertThat(sessionService.authenticate(rawToken).orElseThrow().getPermissions())
                .containsExactly("ADMIN_ROLE_MANAGE");
    }

    private static User activeUser() {
        return User.create(
                USER_ID,
                "EMP-2025-00001",
                "requester",
                "requester@eprocure.local",
                "Request User",
                DEPARTMENT_ID,
                UserStatus.ACTIVE,
                Instant.parse("2026-05-17T00:00:00Z"));
    }

    private static final class FakeSessionCachePort implements SessionCachePort {
        private final Map<String, SessionData> sessions = new HashMap<>();

        @Override
        public void store(SessionData sessionData, Duration ttl) {
            sessions.put(sessionData.tokenHash(), sessionData);
        }

        @Override
        public Optional<SessionData> findByTokenHash(String tokenHash) {
            return Optional.ofNullable(sessions.get(tokenHash));
        }

        @Override
        public void evict(String tokenHash) {
            sessions.remove(tokenHash);
        }
    }

    private static final class FakePermissionCachePort implements PermissionCachePort {
        private final Map<String, Set<String>> permissionsByRole = new HashMap<>();

        @Override
        public Optional<Set<String>> findRolePermissions(String roleCode) {
            return Optional.ofNullable(permissionsByRole.get(roleCode));
        }

        @Override
        public void storeRolePermissions(String roleCode, Set<String> permissionCodes, Duration ttl) {
            permissionsByRole.put(roleCode, Set.copyOf(permissionCodes));
        }

        @Override
        public void evictRolePermissions(String roleCode) {
            permissionsByRole.remove(roleCode);
        }
    }
}
