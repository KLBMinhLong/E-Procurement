## SK-07 · Redis Cache Adapter

### Trigger
Agent thêm caching cho dashboard, session, permission mapping, hoặc bất kỳ data thường xuyên đọc nào.

### Inputs Required
- Key prefix
- TTL requirement
- Data type
- Eviction strategy

### Rules
```
[R1] Key naming: {prefix}:{identifier} — VD: session:{sha256(token)}, perm:{userId}, budget:{deptId}:{year}
[R2] TTL bắt buộc cho phần lớn key, ngoại trừ session/user-session (manual revoke)
[R3] Cache-aside pattern (không write-through mặc định)
[R4] Serialization: JSON (Jackson) — không dùng Java serialization
[R5] Cache failure không chặn business flow — wrap trong try-catch, fallback to DB
[R6] Không cache sensitive data dạng plain text (token phải hash trước khi làm key)
[R7] Eviction: xoá cache khi entity được update (không dùng TTL-only strategy)
```

### Template
```java
package com.eprocure.{service}.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Optional;

/**
 * Redis Adapter: {Feature}CacheAdapter
 * Caches {description}.
 */
@Component
public class {Feature}CacheAdapter {

    private static final Logger log = LogManager.getLogger({Feature}CacheAdapter.class);
    private static final String PREFIX = "{prefix}:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public {Feature}CacheAdapter(RedisTemplate<String, String> redisTemplate,
                                  ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<{DataType}> get(String key) {
        try {
            String raw = redisTemplate.opsForValue().get(PREFIX + key);
            if (raw == null) {
                log.debug("[CACHE] miss | key={}", PREFIX + key);
                return Optional.empty();
            }
            log.debug("[CACHE] hit | key={}", PREFIX + key);
            return Optional.of(objectMapper.readValue(raw, {DataType}.class));
        } catch (Exception e) {
            log.warn("[CACHE] read error | key={} | error={}", PREFIX + key, e.getMessage());
            return Optional.empty();
        }
    }

    public void put(String key, {DataType} value) {
        try {
            String serialized = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(PREFIX + key, serialized, TTL);
            log.debug("[CACHE] put | key={}", PREFIX + key);
        } catch (Exception e) {
            log.warn("[CACHE] write error | key={} | error={}", PREFIX + key, e.getMessage());
        }
    }

    public void evict(String key) {
        try {
            redisTemplate.delete(PREFIX + key);
            log.debug("[CACHE] evict | key={}", PREFIX + key);
        } catch (Exception e) {
            log.warn("[CACHE] evict error | key={} | error={}", PREFIX + key, e.getMessage());
        }
    }
}
```

### Checklist
```
[ ] Key format theo prefix
[ ] TTL được set rõ ràng
[ ] Cache-aside pattern
[ ] Không cache dữ liệu nhạy cảm
```
