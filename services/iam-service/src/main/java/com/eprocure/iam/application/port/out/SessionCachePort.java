package com.eprocure.iam.application.port.out;

import com.eprocure.iam.application.service.SessionData;
import java.time.Duration;
import java.util.Optional;

public interface SessionCachePort {
    void store(SessionData sessionData, Duration ttl);

    Optional<SessionData> findByTokenHash(String tokenHash);

    void evict(String tokenHash);
}
