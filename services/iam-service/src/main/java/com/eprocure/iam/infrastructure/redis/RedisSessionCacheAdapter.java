package com.eprocure.iam.infrastructure.redis;

import com.eprocure.iam.application.port.out.SessionCachePort;
import com.eprocure.iam.application.service.SessionData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisSessionCacheAdapter implements SessionCachePort {
    private static final Logger log = LogManager.getLogger(RedisSessionCacheAdapter.class);
    private static final String KEY_PREFIX = "iam:session:";
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisSessionCacheAdapter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void store(SessionData sessionData, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key(sessionData.tokenHash()), objectMapper.writeValueAsString(sessionData), ttl);
            log.debug("[CACHE] put | key=iam:session:***");
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize session data", exception);
        }
    }

    @Override
    public Optional<SessionData> findByTokenHash(String tokenHash) {
        String value = redisTemplate.opsForValue().get(key(tokenHash));
        if (value == null || value.isBlank()) {
            log.debug("[CACHE] miss | key=iam:session:***");
            return Optional.empty();
        }
        try {
            log.debug("[CACHE] hit | key=iam:session:***");
            return Optional.of(objectMapper.readValue(value, SessionData.class));
        } catch (JsonProcessingException exception) {
            evict(tokenHash);
            return Optional.empty();
        }
    }

    @Override
    public void evict(String tokenHash) {
        redisTemplate.delete(key(tokenHash));
        log.debug("[CACHE] evict | key=iam:session:***");
    }

    private String key(String tokenHash) {
        return KEY_PREFIX + tokenHash;
    }
}
