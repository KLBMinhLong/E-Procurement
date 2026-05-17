package com.eprocure.iam.application.port.out;

import java.time.Duration;

public interface OAuthStateCachePort {
    void store(String stateHash, Duration ttl);

    boolean exists(String stateHash);

    void evict(String stateHash);
}
