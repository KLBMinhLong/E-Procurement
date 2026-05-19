package com.eprocure.pr.application.service;

import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.exception.ErrorCode;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {
    private static final Logger log = LogManager.getLogger(IdempotencyService.class);
    private static final String PREFIX = "idempotent";
    private static final Duration TTL = Duration.ofHours(24);
    private static final int MAX_RESPONSE_BYTES = 512 * 1024;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void verify(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.SYS_005);
        }
        try {
            UUID key = UUID.fromString(idempotencyKey);
            if (key.version() != 4 || !idempotencyKey.equals(idempotencyKey.toLowerCase(Locale.ROOT))) {
                throw new BusinessException(ErrorCode.SYS_005);
            }
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.SYS_005);
        }
    }

    public <T> Optional<T> find(String operation, UUID actorId, String idempotencyKey, Class<T> type) {
        String cacheKey = cacheKey(operation, actorId, idempotencyKey);
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(cacheKey))
                    .map(value -> readValue(value, type));
        } catch (RuntimeException exception) {
            log.warn("[CACHE] miss idempotency | key={} | reason={}",
                    LogMaskingUtil.maskToken(idempotencyKey),
                    exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public void save(String operation, UUID actorId, String idempotencyKey, Object response) {
        String cacheKey = cacheKey(operation, actorId, idempotencyKey);
        try {
            String serialized = objectMapper.writeValueAsString(response);
            if (serialized.getBytes().length <= MAX_RESPONSE_BYTES) {
                redisTemplate.opsForValue().set(cacheKey, serialized, TTL);
                log.debug("[CACHE] put idempotency | key={}", LogMaskingUtil.maskToken(idempotencyKey));
            }
        } catch (Exception exception) {
            log.warn("[CACHE] put idempotency failed | key={} | reason={}",
                    LogMaskingUtil.maskToken(idempotencyKey),
                    exception.getClass().getSimpleName());
        }
    }

    private <T> T readValue(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read idempotency cache", exception);
        }
    }

    private String cacheKey(String operation, UUID actorId, String idempotencyKey) {
        return String.join(":", PREFIX, operation, actorId.toString(), idempotencyKey);
    }
}
