package com.eprocure.iam.application.port.out;

import com.eprocure.iam.application.service.TwoFactorChallengeData;
import java.time.Duration;
import java.util.Optional;

public interface TwoFactorChallengeCachePort {
    void store(TwoFactorChallengeData challengeData, Duration ttl);

    Optional<TwoFactorChallengeData> findByTokenHash(String tokenHash);

    void evict(String tokenHash);
}
