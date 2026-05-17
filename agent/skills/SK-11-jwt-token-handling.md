## SK-11 · JWT / Token Handling

### Trigger
Agent implement auth flow, token generation, session management.

### Inputs Required
- Redis + DB session repository
- Token hash strategy
- Session data fields

### Rules
```
[R1] Token là OPAQUE string (UUID hash) — không phải JWT có payload
[R2] Token không hết hạn theo thời gian — chỉ bị thu hồi khi logout/re-login
[R3] 1 phiên đăng nhập duy nhất — login mới invalidate token cũ
[R4] Token lưu trong: Redis (primary, fast lookup) + DB (backup, audit)
[R5] FE nhận token qua HttpOnly Cookie (không accessible từ JS)
[R6] Session data trong Redis: { userId, roles, permissions, loginAt, deviceInfo }
[R7] Token format trong Redis key: session:{sha256(token)}
[R8] Không trả JWT ra FE — token là chuỗi vô nghĩa
```

### Template
```java
@Service
public class TokenService {

    private static final Logger log = LogManager.getLogger(TokenService.class);
    private static final String SESSION_PREFIX = "session:";

    private final RedisTemplate<String, String> redisTemplate;
    private final SessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Sinh token mới khi login thành công.
     * Invalidate token cũ nếu tồn tại.
     */
    public String createSession(UUID userId, Set<String> roles, Set<String> permissions) {
        // 1. Invalidate old session
        invalidateExistingSession(userId);

        // 2. Generate opaque token
        String rawToken = UUID.randomUUID().toString().replace("-", "") +
                          UUID.randomUUID().toString().replace("-", "");
        String tokenKey = hashToken(rawToken);

        // 3. Build session data
        SessionData session = SessionData.builder()
            .userId(userId)
            .roles(roles)
            .permissions(permissions)
            .loginAt(Instant.now())
            .build();

        // 4. Save to Redis (no expiry — manual revocation only)
        String sessionJson = objectMapper.writeValueAsString(session);
        redisTemplate.opsForValue().set(SESSION_PREFIX + tokenKey, sessionJson);

        // 5. Save to DB for audit
        sessionRepository.save(new SessionEntity(tokenKey, userId, Instant.now()));

        // 6. Track userId → tokenKey mapping for single-session enforcement
        redisTemplate.opsForValue().set("user-session:" + userId, tokenKey);

        log.info("[TOKEN] Session created | userId={}", userId);
        return rawToken;  // Return raw token to set in HttpOnly Cookie
    }

    /**
     * Validate token và trả về SessionData.
     */
    public Optional<SessionData> validateToken(String rawToken) {
        String tokenKey = hashToken(rawToken);
        String sessionJson = redisTemplate.opsForValue().get(SESSION_PREFIX + tokenKey);

        if (sessionJson == null) {
            log.warn("[TOKEN] Invalid or expired token | key={}", tokenKey.substring(0, 8) + "...");
            return Optional.empty();
        }

        return Optional.of(objectMapper.readValue(sessionJson, SessionData.class));
    }

    public void invalidateToken(String rawToken) {
        String tokenKey = hashToken(rawToken);
        redisTemplate.delete(SESSION_PREFIX + tokenKey);
        sessionRepository.markRevoked(tokenKey, Instant.now());
        log.info("[TOKEN] Session revoked | key={}", tokenKey.substring(0, 8) + "...");
    }

    private void invalidateExistingSession(UUID userId) {
        String existingKey = redisTemplate.opsForValue().get("user-session:" + userId);
        if (existingKey != null) {
            redisTemplate.delete(SESSION_PREFIX + existingKey);
            redisTemplate.delete("user-session:" + userId);
            log.info("[TOKEN] Previous session invalidated | userId={}", userId);
        }
    }

    private String hashToken(String rawToken) {
        // SHA-256 hash — token key không expose raw token
        return DigestUtils.sha256Hex(rawToken);
    }
}
```

### Checklist
```
[ ] Token là opaque string, không dùng JWT payload
[ ] Single-session enforcement
[ ] Token lưu Redis + DB (audit)
[ ] HttpOnly Cookie cho FE
```
