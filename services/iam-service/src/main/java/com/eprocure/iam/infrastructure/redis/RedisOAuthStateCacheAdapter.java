package com.eprocure.iam.infrastructure.redis;

import com.eprocure.iam.application.port.out.OAuthStateCachePort;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisOAuthStateCacheAdapter implements OAuthStateCachePort {
    private static final Logger log = LoggerFactory.getLogger(RedisOAuthStateCacheAdapter.class);
    private static final String KEY_PREFIX = "iam:oauth:state:";

    private final StringRedisTemplate redisTemplate;

    public RedisOAuthStateCacheAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void store(String stateHash, Duration ttl) {
        redisTemplate.opsForValue().set(key(stateHash), "1", ttl);
        log.debug("[CACHE] put | key=iam:oauth:state:***");
    }

    @Override
    public boolean exists(String stateHash) {
        Boolean exists = redisTemplate.hasKey(key(stateHash));
        log.debug("[CACHE] {} | key=iam:oauth:state:***", Boolean.TRUE.equals(exists) ? "hit" : "miss");
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void evict(String stateHash) {
        redisTemplate.delete(key(stateHash));
        log.debug("[CACHE] evict | key=iam:oauth:state:***");
    }

    private String key(String stateHash) {
        return KEY_PREFIX + stateHash;
    }
}
