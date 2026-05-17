package com.eprocure.iam.infrastructure.redis;

import com.eprocure.iam.application.port.out.TwoFactorChallengeCachePort;
import com.eprocure.iam.application.service.TwoFactorChallengeData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisTwoFactorChallengeCacheAdapter implements TwoFactorChallengeCachePort {
    private static final Logger log = LoggerFactory.getLogger(RedisTwoFactorChallengeCacheAdapter.class);
    private static final String KEY_PREFIX = "iam:2fa:challenge:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisTwoFactorChallengeCacheAdapter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void store(TwoFactorChallengeData challengeData, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(
                    key(challengeData.tokenHash()),
                    objectMapper.writeValueAsString(challengeData),
                    ttl);
            log.debug("[CACHE] put | key=iam:2fa:challenge:***");
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize 2FA challenge data", exception);
        }
    }

    @Override
    public Optional<TwoFactorChallengeData> findByTokenHash(String tokenHash) {
        String value = redisTemplate.opsForValue().get(key(tokenHash));
        if (value == null || value.isBlank()) {
            log.debug("[CACHE] miss | key=iam:2fa:challenge:***");
            return Optional.empty();
        }
        try {
            log.debug("[CACHE] hit | key=iam:2fa:challenge:***");
            return Optional.of(objectMapper.readValue(value, TwoFactorChallengeData.class));
        } catch (JsonProcessingException exception) {
            evict(tokenHash);
            return Optional.empty();
        }
    }

    @Override
    public void evict(String tokenHash) {
        redisTemplate.delete(key(tokenHash));
        log.debug("[CACHE] evict | key=iam:2fa:challenge:***");
    }

    private String key(String tokenHash) {
        return KEY_PREFIX + tokenHash;
    }
}
