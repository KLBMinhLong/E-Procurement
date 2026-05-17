## SK-26 · Idempotency Handler

### Trigger
Agent cần implement idempotency cho POST/PATCH/PUT.

### Inputs Required
- Idempotency-Key header name
- TTL requirement
- Response size limit

### Rules
```
[R1] Mọi POST/PATCH endpoint nhận Idempotency-Key header (UUID format)
[R2] Key lưu trong Redis với TTL 24h
[R3] Nếu key đã tồn tại → trả về response đã cache, không execute lại
[R4] Key scope: per-user per-operation (không cross-user)
[R5] FE Angular tự động gán UUID header qua idempotencyInterceptor
[R6] Cache response ≤ 512KB — response lớn hơn chỉ cache status code
```

### Template
```java
@Service
public class IdempotencyService {

    private static final String PREFIX = "idempotent:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, String> redis;
    private final ObjectMapper objectMapper;

    /**
     * Thực thi supplier hoặc trả về cached result nếu key đã xử lý.
     */
    public <T> T getOrExecute(String idempotencyKey, Class<T> type,
                               Supplier<T> supplier) {
        String redisKey = PREFIX + idempotencyKey;

        // Check cache
        String cached = redis.opsForValue().get(redisKey);
        if (cached != null) {
            log.info("[IDEMPOTENCY] Cache hit | key={}", idempotencyKey);
            return objectMapper.readValue(cached, type);
        }

        // Execute
        T result = supplier.get();

        // Cache result
        try {
            String serialized = objectMapper.writeValueAsString(result);
            if (serialized.length() <= 512 * 1024) { // 512KB limit
                redis.opsForValue().set(redisKey, serialized, TTL);
            }
        } catch (Exception e) {
            log.warn("[IDEMPOTENCY] Cache write failed | key={}", idempotencyKey);
        }

        return result;
    }

    public boolean hasProcessed(String key) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + key));
    }

    public void markProcessed(String key) {
        redis.opsForValue().set(PREFIX + key, "1", TTL);
    }
}
```

### Checklist
```
[ ] Key được cache với TTL 24h
[ ] Scope per-user per-operation
[ ] Response cache <= 512KB
[ ] Idempotency-Key bắt buộc cho POST/PATCH
```
