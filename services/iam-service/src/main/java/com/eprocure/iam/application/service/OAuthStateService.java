package com.eprocure.iam.application.service;

import com.eprocure.iam.application.port.out.OAuthStateCachePort;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OAuthStateService {
    private final OAuthStateCachePort stateCachePort;
    private final OpaqueTokenService opaqueTokenService;
    private final Duration stateTtl;

    public OAuthStateService(
            OAuthStateCachePort stateCachePort,
            OpaqueTokenService opaqueTokenService,
            @Value("${eprocure.google-oauth.state-ttl-minutes:5}") long stateTtlMinutes) {
        this.stateCachePort = stateCachePort;
        this.opaqueTokenService = opaqueTokenService;
        this.stateTtl = Duration.ofMinutes(stateTtlMinutes);
    }

    public String create() {
        String state = opaqueTokenService.generate();
        stateCachePort.store(opaqueTokenService.hash(state), stateTtl);
        return state;
    }

    public boolean consume(String state, String stateCookie) {
        if (state == null || state.isBlank() || stateCookie == null || stateCookie.isBlank()) {
            return false;
        }
        if (!constantTimeEquals(state, stateCookie)) {
            return false;
        }
        String stateHash = opaqueTokenService.hash(state);
        boolean exists = stateCachePort.exists(stateHash);
        if (exists) {
            stateCachePort.evict(stateHash);
        }
        return exists;
    }

    public Duration stateTtl() {
        return stateTtl;
    }

    private boolean constantTimeEquals(String first, String second) {
        byte[] firstBytes = first.getBytes(StandardCharsets.UTF_8);
        byte[] secondBytes = second.getBytes(StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(firstBytes, secondBytes);
    }
}
