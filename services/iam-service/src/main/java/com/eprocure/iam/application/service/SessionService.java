package com.eprocure.iam.application.service;

import com.eprocure.iam.application.port.in.ClientContext;
import com.eprocure.iam.application.port.out.SessionCachePort;
import com.eprocure.iam.common.security.UserPrincipal;
import com.eprocure.iam.domain.model.SessionRecord;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.SessionRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SessionService {
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final SessionCachePort sessionCachePort;
    private final OpaqueTokenService opaqueTokenService;
    private final PermissionResolutionService permissionResolutionService;
    private final Duration sessionTtl;

    public SessionService(
            SessionRepository sessionRepository,
            UserRepository userRepository,
            SessionCachePort sessionCachePort,
            OpaqueTokenService opaqueTokenService,
            PermissionResolutionService permissionResolutionService,
            @Value("${eprocure.session.ttl-hours:8}") long sessionTtlHours) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.sessionCachePort = sessionCachePort;
        this.opaqueTokenService = opaqueTokenService;
        this.permissionResolutionService = permissionResolutionService;
        this.sessionTtl = Duration.ofHours(sessionTtlHours);
    }

    public CreatedSession issueFor(User user, ClientContext clientContext) {
        Instant now = Instant.now();
        String rawToken = opaqueTokenService.generate();
        String tokenHash = opaqueTokenService.hash(rawToken);
        Instant expiresAt = now.plus(sessionTtl);

        revokeActiveForUser(user.getId(), user.getId(), now);
        SessionRecord sessionRecord = SessionRecord.issue(
                UUID.randomUUID(),
                user.getId(),
                tokenHash,
                clientContext.ipAddress(),
                clientContext.userAgent(),
                now,
                expiresAt);
        sessionRepository.save(sessionRecord);

        Set<String> roles = userRepository.findRoleCodesByUserId(user.getId());
        sessionCachePort.store(new SessionData(tokenHash, user.getId(), expiresAt, roles), sessionTtl);
        return new CreatedSession(rawToken, tokenHash, expiresAt, roles);
    }

    public Optional<UserPrincipal> authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        String tokenHash = opaqueTokenService.hash(rawToken);
        Instant now = Instant.now();
        Optional<SessionData> cachedSession = sessionCachePort.findByTokenHash(tokenHash)
                .filter(sessionData -> sessionData.isActive(now));
        if (cachedSession.isPresent()) {
            return toPrincipal(cachedSession.get(), tokenHash);
        }

        return sessionRepository.findActiveByTokenHash(tokenHash, now)
                .flatMap(sessionRecord -> {
                    Set<String> roles = userRepository.findRoleCodesByUserId(sessionRecord.getUserId());
                    SessionData sessionData = new SessionData(
                            tokenHash,
                            sessionRecord.getUserId(),
                            sessionRecord.getExpiresAt(),
                            roles);
                    sessionCachePort.store(sessionData, Duration.between(now, sessionRecord.getExpiresAt()));
                    return toPrincipal(sessionData, tokenHash);
                });
    }

    public void revoke(String rawToken, UUID revokedBy) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String tokenHash = opaqueTokenService.hash(rawToken);
        sessionRepository.revokeByTokenHash(tokenHash, revokedBy, Instant.now());
        sessionCachePort.evict(tokenHash);
    }

    public void revokeActiveForUser(UUID userId, UUID revokedBy, Instant revokedAt) {
        sessionRepository.findActiveTokenHashesByUserId(userId, revokedAt).forEach(sessionCachePort::evict);
        sessionRepository.revokeActiveByUserId(userId, revokedBy, revokedAt);
    }

    public void evictActiveCacheForUser(UUID userId) {
        sessionRepository.findActiveTokenHashesByUserId(userId, Instant.now()).forEach(sessionCachePort::evict);
    }

    private Optional<UserPrincipal> toPrincipal(SessionData sessionData, String tokenHash) {
        return userRepository.findById(sessionData.userId())
                .filter(User::canLogin)
                .map(user -> UserPrincipal.from(
                        user,
                        tokenHash,
                        permissionResolutionService.resolveByRoleCodes(sessionData.roles())));
    }
}
