# IDEMPOTENCY
## eProcure Enterprise — Tránh xử lý trùng lặp

---

## 1. VẤN ĐỀ CẦN GIẢI QUYẾT

Trong môi trường phân tán, các tình huống sau có thể gây xử lý trùng lặp:

- **Network timeout**: FE gửi request, network timeout, FE retry → BE nhận 2 lần
- **Double-click**: User bấm Submit 2 lần nhanh
- **Mobile reconnect**: App mất mạng, tự reconnect và retry
- **Kafka redelivery**: Consumer xử lý lại message đã consume

---

## 2. IDEMPOTENCY-KEY HEADER

### 2.1 Quy tắc áp dụng

| Method | Bắt buộc Idempotency-Key | Ghi chú |
|---|---|---|
| `GET` | ❌ | Safe + Idempotent by nature |
| `POST` | ✅ Bắt buộc | Tạo mới hoặc action |
| `PUT` | ✅ Bắt buộc | Update toàn bộ |
| `PATCH` | ✅ Bắt buộc | Đổi trạng thái |

### 2.2 Format

```http
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

- Format: UUID v4 (lowercase, with hyphens)
- FE tạo mới cho mỗi **ý định hành động** — không phải mỗi HTTP attempt
- Cùng một ý định dùng cùng key khi retry

```typescript
// Angular — tạo key khi user thực hiện action, giữ nguyên khi retry
class PurchaseRequestService {
  private pendingIdempotencyKey: string | null = null;

  // Gọi khi user bấm "Tạo yêu cầu" lần đầu
  initNewPrAction(): void {
    this.pendingIdempotencyKey = crypto.randomUUID();
  }

  // Gọi khi submit (lần đầu hoặc retry)
  async createPr(data: CreatePrRequest): Promise<void> {
    const key = this.pendingIdempotencyKey ?? crypto.randomUUID();
    await this.http.post('/api/v1/purchase-requests', data, {
      headers: { 'Idempotency-Key': key }
    }).toPromise();
    this.pendingIdempotencyKey = null; // Reset sau khi thành công
  }
}
```

---

## 3. BACKEND IMPLEMENTATION

### 3.1 Flow xử lý

```
Request đến với Idempotency-Key: {key}
        │
        ▼
Check Redis: GET idempotent:{key}
        │
   ┌────┴────┐
   │  HIT    │  MISS
   ▼         ▼
Trả về    Proceed với business logic
cached    → Lưu result vào Redis
response  → Trả về result mới
(200)
```

### 3.2 Redis Storage

```
Key:   idempotent:{uuid}
Value: {
  "statusCode": 201,
  "body": { "success": true, "code": "PR_CREATED", "data": { ... } },
  "createdAt": "2025-01-15T08:30:00Z"
}
TTL:   24 giờ
```

### 3.3 IdempotencyService

```java
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Logger log = LogManager.getLogger(IdempotencyService.class);
    private static final Duration TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "idempotent:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Kiểm tra cache idempotency. Nếu có → trả về response đã cache.
     */
    public Optional<CachedResponse> check(String idempotencyKey) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached == null) {
            log.debug("[CACHE] miss | key=idempotent:{}", idempotencyKey.substring(0, 8));
            return Optional.empty();
        }
        log.info("[CACHE] hit | key=idempotent:{}", idempotencyKey.substring(0, 8));
        try {
            return Optional.of(objectMapper.readValue(cached, CachedResponse.class));
        } catch (JsonProcessingException e) {
            log.warn("[CACHE] corrupt idempotency cache, ignoring | key={}", idempotencyKey);
            return Optional.empty();
        }
    }

    /**
     * Lưu kết quả vào cache sau khi xử lý thành công.
     */
    public void save(String idempotencyKey, int statusCode, Object responseBody) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        try {
            CachedResponse cached = new CachedResponse(statusCode,
                objectMapper.writeValueAsString(responseBody), Instant.now());
            redisTemplate.opsForValue().set(redisKey,
                objectMapper.writeValueAsString(cached), TTL);
            log.debug("[CACHE] put | key=idempotent:{} | ttl=24h", idempotencyKey.substring(0, 8));
        } catch (JsonProcessingException e) {
            log.error("[CACHE] failed to save idempotency | key={}", idempotencyKey, e);
        }
    }
}

public record CachedResponse(int statusCode, String body, Instant createdAt) {}
```

### 3.4 Interceptor tự động (Global)

```java
@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final IdempotencyService idempotencyService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {
        // Chỉ áp dụng cho POST, PUT, PATCH
        String method = request.getMethod();
        if (!Set.of("POST", "PUT", "PATCH").contains(method)) return true;

        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank()) {
            // Một số endpoint internal không cần (vd: /auth/login)
            // Đánh dấu để UseCase biết skip idempotency check
            return true;
        }

        // Validate UUID format
        if (!key.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) {
            response.setStatus(400);
            // write error response
            return false;
        }

        Optional<CachedResponse> cached = idempotencyService.check(key);
        if (cached.isPresent()) {
            CachedResponse cr = cached.get();
            response.setStatus(cr.statusCode());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(cr.body());
            return false; // Không tiếp tục xử lý
        }

        // Lưu key vào request attribute để UseCase dùng sau khi xử lý
        request.setAttribute("idempotencyKey", key);
        return true;
    }
}
```

---

## 4. KAFKA CONSUMER IDEMPOTENCY

```java
@KafkaListener(topics = "procurement.pr.submitted")
public void onPrSubmitted(ConsumerRecord<String, String> record) {
    String messageId = record.headers().lastHeader("messageId").toString();
    String redisKey = "kafka-consumed:" + messageId;

    // Kiểm tra đã xử lý chưa
    if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
        log.info("[CACHE] Kafka duplicate skip | messageId={}", messageId);
        return;
    }

    try {
        processMessage(record.value());
        // Đánh dấu đã xử lý (TTL 48h)
        redisTemplate.opsForValue().set(redisKey, "1", Duration.ofHours(48));
    } catch (Exception e) {
        log.error("[EXCEPTION][APR_007] Kafka processing failed | messageId={}", messageId, e);
        throw e; // Kafka sẽ retry
    }
}
```

---

## 5. RESPONSE KHI IDEMPOTENCY HIT

```http
HTTP/1.1 200 OK
Idempotency-Replayed: true           ← Header để FE biết đây là cached response

{
  "success": true,
  "code": "PR_CREATED",
  "data": { ... }                    ← Giống hệt response lần đầu
}
```

> Nếu request đầu tiên **thất bại** (5xx) → không cache → request tiếp theo sẽ retry thực sự.  
> Nếu request đầu tiên **thành công** (2xx) → cache 24h → mọi retry đều trả về cached response.  
> Nếu request đầu tiên **đang xử lý** (concurrent) → trả về `409 Conflict` với gợi ý retry sau.

---

## 6. MISSING KEY BEHAVIOR

Endpoint nào **bắt buộc** Idempotency-Key mà không có → `400 Bad Request`:

```json
{
  "success": false,
  "code": "SYS_005",
  "message": "Header 'Idempotency-Key' bắt buộc cho request này"
}
```

Endpoint **không bắt buộc** (auth, file upload, etc.) → bỏ qua check.
