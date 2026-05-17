package com.eprocure.iam.application.service;

import com.eprocure.iam.application.port.in.ClientContext;
import com.eprocure.iam.application.port.out.TwoFactorChallengeCachePort;
import com.eprocure.iam.domain.model.User;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TwoFactorChallengeService {
    private final TwoFactorChallengeCachePort challengeCachePort;
    private final OpaqueTokenService opaqueTokenService;
    private final Duration challengeTtl;

    public TwoFactorChallengeService(
            TwoFactorChallengeCachePort challengeCachePort,
            OpaqueTokenService opaqueTokenService,
            @Value("${eprocure.two-factor.challenge-ttl-minutes:5}") long challengeTtlMinutes) {
        this.challengeCachePort = challengeCachePort;
        this.opaqueTokenService = opaqueTokenService;
        this.challengeTtl = Duration.ofMinutes(challengeTtlMinutes);
    }

    public CreatedTwoFactorChallenge createFor(User user, ClientContext clientContext) {
        Instant expiresAt = Instant.now().plus(challengeTtl);
        String rawToken = opaqueTokenService.generate();
        String tokenHash = opaqueTokenService.hash(rawToken);
        TwoFactorChallengeData challengeData = new TwoFactorChallengeData(
                tokenHash,
                user.getId(),
                clientContext.ipAddress(),
                clientContext.userAgent(),
                expiresAt);
        challengeCachePort.store(challengeData, challengeTtl);
        return new CreatedTwoFactorChallenge(rawToken, tokenHash, expiresAt);
    }

    public Optional<TwoFactorChallengeData> findByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        String tokenHash = opaqueTokenService.hash(rawToken);
        return challengeCachePort.findByTokenHash(tokenHash)
                .filter(challengeData -> challengeData.isActive(Instant.now()));
    }

    public void evict(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        challengeCachePort.evict(opaqueTokenService.hash(rawToken));
    }

    public Duration challengeTtl() {
        return challengeTtl;
    }
}
