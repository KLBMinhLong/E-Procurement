package com.eprocure.iam.infrastructure.redis;

import com.eprocure.iam.application.port.out.PermissionCachePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisPermissionCacheAdapter implements PermissionCachePort {
    private static final Logger log = LogManager.getLogger(RedisPermissionCacheAdapter.class);
    private static final String ROLE_PERMISSION_KEY_PREFIX = "iam:role-perm:";
    private static final TypeReference<Set<String>> PERMISSION_SET_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisPermissionCacheAdapter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Set<String>> findRolePermissions(String roleCode) {
        String value = redisTemplate.opsForValue().get(rolePermissionKey(roleCode));
        if (value == null || value.isBlank()) {
            log.debug("[CACHE] miss | key=iam:role-perm:{}", roleCode);
            return Optional.empty();
        }
        try {
            log.debug("[CACHE] hit | key=iam:role-perm:{}", roleCode);
            return Optional.of(Set.copyOf(objectMapper.readValue(value, PERMISSION_SET_TYPE)));
        } catch (JsonProcessingException exception) {
            evictRolePermissions(roleCode);
            return Optional.empty();
        }
    }

    @Override
    public void storeRolePermissions(String roleCode, Set<String> permissionCodes, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(
                    rolePermissionKey(roleCode),
                    objectMapper.writeValueAsString(Set.copyOf(permissionCodes)),
                    ttl);
            log.debug("[CACHE] put | key=iam:role-perm:{} | ttl={}s", roleCode, ttl.toSeconds());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize role permissions", exception);
        }
    }

    @Override
    public void evictRolePermissions(String roleCode) {
        redisTemplate.delete(rolePermissionKey(roleCode));
        log.debug("[CACHE] evict | key=iam:role-perm:{}", roleCode);
    }

    private String rolePermissionKey(String roleCode) {
        return ROLE_PERMISSION_KEY_PREFIX + roleCode;
    }
}
