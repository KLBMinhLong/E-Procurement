# CODING GUIDE
## eProcure Enterprise — Quy chuẩn code

---

> **Version:** 1.0.0  
> Tài liệu này là quy tắc bắt buộc cho tất cả thành viên team.  
> "Nếu không có quy tắc nào cover trường hợp của bạn → hỏi Tech Lead trước khi code."

---

## 1. PROJECT STRUCTURE (Spring Boot Service)

```
{service-name}/
├── src/
│   ├── main/
│   │   ├── java/com/eprocure/{service}/
│   │   │   ├── domain/                        # POJO thuần, ZERO framework dependency
│   │   │   │   ├── model/                     # Entity, Value Object, Aggregate Root
│   │   │   │   │   ├── PurchaseRequest.java
│   │   │   │   │   ├── PrLineItem.java
│   │   │   │   │   └── vo/                    # Value Objects
│   │   │   │   │       ├── Money.java
│   │   │   │   │       └── PrNumber.java
│   │   │   │   ├── repository/                # Repository Interfaces (Port)
│   │   │   │   │   └── PurchaseRequestRepository.java
│   │   │   │   ├── service/                   # Domain Services
│   │   │   │   │   └── BudgetCalculationService.java
│   │   │   │   └── event/                     # Domain Events
│   │   │   │       └── PrSubmittedEvent.java
│   │   │   │
│   │   │   ├── application/                   # Use Cases / Application Services
│   │   │   │   ├── usecase/
│   │   │   │   │   ├── CreatePurchaseRequestUseCase.java
│   │   │   │   │   ├── SubmitPurchaseRequestUseCase.java
│   │   │   │   │   └── ApprovePurchaseRequestUseCase.java
│   │   │   │   ├── port/                      # Input/Output Ports (interfaces)
│   │   │   │   │   ├── in/
│   │   │   │   │   │   └── CreatePrCommand.java
│   │   │   │   │   └── out/
│   │   │   │   │       └── BudgetServicePort.java  # Call ra Finance Service
│   │   │   │   └── dto/                       # Application-level DTOs (internal)
│   │   │   │
│   │   │   ├── infrastructure/                # Spring, DB, Kafka, Redis
│   │   │   │   ├── persistence/
│   │   │   │   │   ├── mapper/               # MyBatis Mappers
│   │   │   │   │   │   └── PurchaseRequestMapper.java
│   │   │   │   │   ├── entity/               # MyBatis result entities (DB mapping)
│   │   │   │   │   │   └── PurchaseRequestEntity.java
│   │   │   │   │   └── repository/           # Repository Implementations
│   │   │   │   │       └── PurchaseRequestRepositoryImpl.java
│   │   │   │   ├── kafka/
│   │   │   │   │   ├── producer/
│   │   │   │   │   └── consumer/
│   │   │   │   ├── redis/
│   │   │   │   │   └── RedisSessionAdapter.java
│   │   │   │   ├── http/                     # FeignClient / RestTemplate cho inter-service call
│   │   │   │   │   └── FinanceServiceClient.java
│   │   │   │   └── config/
│   │   │   │       ├── MyBatisConfig.java
│   │   │   │       ├── KafkaConfig.java
│   │   │   │       ├── RedisConfig.java
│   │   │   │       └── SecurityConfig.java
│   │   │   │
│   │   │   ├── presentation/                  # Controllers, Request/Response DTOs
│   │   │   │   ├── controller/
│   │   │   │   │   └── PurchaseRequestController.java
│   │   │   │   ├── request/
│   │   │   │   │   └── CreatePrRequest.java
│   │   │   │   ├── response/
│   │   │   │   │   └── PurchaseRequestResponse.java
│   │   │   │   └── mapper/                   # Presentation Mapper (Request/Response ↔ Command/DTO)
│   │   │   │       └── PrPresentationMapper.java
│   │   │   │
│   │   │   └── common/                        # Constants, Exceptions, Utils
│   │   │       ├── constant/
│   │   │       │   ├── PrErrorCode.java       # Mã lỗi service PR
│   │   │       │   ├── PrStatus.java
│   │   │       │   └── PrPriority.java
│   │   │       ├── exception/
│   │   │       │   ├── BusinessException.java  # Base class
│   │   │       │   ├── PrNotFoundException.java
│   │   │       │   └── InsufficientBudgetException.java
│   │   │       └── util/
│   │   │           ├── PrNumberGenerator.java
│   │   │           └── LogMaskingUtil.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── log4j2.xml
│   │       └── db/migration/
│   │           └── V1__create_pr_schema.sql
│   │
│   └── test/
│       ├── java/com/eprocure/{service}/
│       │   ├── domain/                        # Unit tests cho domain
│       │   ├── application/                   # Unit tests cho use cases
│       │   └── presentation/                  # Integration tests cho controllers
│       └── resources/
│           └── application-test.yml
│
├── Dockerfile
├── pom.xml
└── README.md
```

---

## 2. NAMING CONVENTIONS

### 2.1 Java

```java
// Package: lowercase, hyphen → underscore
com.eprocure.purchase_request  ❌
com.eprocure.pr                 ✅

// Class: PascalCase
purchaseRequestService          ❌
PurchaseRequestService          ✅

// Method, variable: camelCase
GetTotalAmount()                ❌
getTotalAmount()                ✅

// Constant: UPPER_SNAKE_CASE
private static final String maxRetry = "3";   ❌
private static final int MAX_RETRY = 3;        ✅

// Enum values: UPPER_SNAKE_CASE
enum PrStatus { draft, pendingApproval }   ❌
enum PrStatus { DRAFT, PENDING_APPROVAL }  ✅

// Use Case: [Verb][Noun]UseCase
PurchaseRequestCreator          ❌
CreatePurchaseRequestUseCase    ✅

// Repository Interface: [Entity]Repository
PurchaseRequestRepo             ❌
PurchaseRequestRepository       ✅
```

### 2.2 Database

```sql
-- Table names: lowercase_snake_case, plural
PurchaseRequests               ❌
purchase_requests              ✅

-- Column names: lowercase_snake_case
prNumber                       ❌
pr_number                      ✅

-- Index names: idx_{table}_{columns}
idx_pr_requester_id            ✅
idx_pr_dept_status             ✅ (composite)

-- FK constraint: fk_{table}_{referenced_table}
fk_pr_line_items_pr            ✅
```

### 2.3 API Endpoints

```
# RESTful, lowercase, hyphen-separated, plural nouns
GET  /api/v1/purchaseRequests      ❌
GET  /api/v1/purchase-requests     ✅

# Action không fit CRUD → verb ở cuối
POST /api/v1/purchase-requests/{id}/submit     ✅
POST /api/v1/purchase-requests/{id}/cancel     ✅
POST /api/v1/approvals/tasks/{id}/approve      ✅

# Không có DELETE
DELETE /api/v1/purchase-requests/{id}          ❌
PATCH  /api/v1/purchase-requests/{id}/cancel   ✅
```

### 2.4 Kafka Topics

```
{domain}.{entity}.{event}

procurement.pr.submitted
procurement.pr.approved
procurement.pr.rejected
approval.step.assigned
approval.sla.breached
finance.budget.warning
inventory.gr.created
```

### 2.5 Redis Keys

```
session:{tokenHash}                      → User session data with roles only (TTL sliding)
role-perm:{roleCode}                     → Set<permissionCode> cho role
idempotent:{idempotencyKey}              → Cached response (TTL 24h)
budget:{departmentId}:{fiscalYear}:{q}   → Budget snapshot
dashboard:manager:{userId}               → Dashboard data (TTL 5min)
```

---

## 3. CONTROLLER RULES

```java
@RestController
@RequestMapping("/api/v1/purchase-requests")
@RequiredArgsConstructor
public class PurchaseRequestController {

    private static final Logger log = LogManager.getLogger(PurchaseRequestController.class);

    private final CreatePurchaseRequestUseCase createPurchaseRequestUseCase;
    private final PrPresentationMapper presentationMapper;

    /**
     * Tạo yêu cầu mua sắm mới.
     * Yêu cầu quyền: PR_CREATE
     *
     * @param request Thông tin PR mới
     * @param currentUser Thông tin người dùng hiện tại (inject từ SecurityContext)
     * @return PR đã tạo với số PR tự động
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PR_CREATE')")              // ✅ Permission code, không phải role
    public ResponseEntity<ApiResponse<PurchaseRequestResponse>> createPr(
            @Valid @RequestBody CreatePrRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        log.info("[CONTROLLER] POST /purchase-requests | userId={}", currentUser.getMaskedId());

        CreatePrCommand command = presentationMapper.toCommand(request, currentUser.getId());
        PurchaseRequest pr = createPurchaseRequestUseCase.execute(command, idempotencyKey);
        PurchaseRequestResponse response = presentationMapper.toResponse(pr);

        log.info("[CONTROLLER] PR created | prNumber={}", pr.getPrNumber());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("PR_CREATED", response));
    }
}
```

**Controller Rules:**
- Chỉ chứa: mapping, validation, `@PreAuthorize`, log entry/exit, delegate sang UseCase
- KHÔNG chứa business logic
- KHÔNG dùng `@Autowired` — dùng constructor injection qua `@RequiredArgsConstructor`
- Luôn dùng `@Valid` cho request body
- Log `[CONTROLLER]` level INFO khi vào/ra

---

## 4. USE CASE RULES

```java
/**
 * Use Case: Tạo Purchase Request mới.
 *
 * <p>Flow:
 * <ol>
 *   <li>Validate business rules</li>
 *   <li>Check idempotency</li>
 *   <li>Generate PR number</li>
 *   <li>Persist</li>
 *   <li>Return domain object</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class CreatePurchaseRequestUseCase {

    private static final Logger log = LogManager.getLogger(CreatePurchaseRequestUseCase.class);

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PrNumberGenerator prNumberGenerator;
    private final IdempotencyService idempotencyService;

    @Transactional
    public PurchaseRequest execute(CreatePrCommand command, String idempotencyKey) {
        // 1. Idempotency check
        Optional<PurchaseRequest> cached = idempotencyService.get(idempotencyKey, PurchaseRequest.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit | key={}", idempotencyKey);
            return cached.get();
        }

        log.info("[ACTION] Start CreatePR | userId={}", command.getRequesterId());

        // 2. Business validation
        validateCommand(command);

        // 3. Build domain object
        String prNumber = prNumberGenerator.next();
        PurchaseRequest pr = PurchaseRequest.create(command, prNumber);

        // 4. Persist
        purchaseRequestRepository.save(pr);

        // 5. Cache idempotency result
        idempotencyService.set(idempotencyKey, pr);

        log.info("[ACTION] Complete CreatePR | prNumber={} | userId={}", prNumber, command.getRequesterId());
        return pr;
    }

    private void validateCommand(CreatePrCommand command) {
        if (command.getLineItems() == null || command.getLineItems().isEmpty()) {
            throw new ValidationException("PR_001", "PR phải có ít nhất 1 mục hàng hoá");
        }
    }
}
```

**UseCase Rules:**
- Mỗi UseCase = 1 hành động nghiệp vụ, 1 method `execute()`
- Không call UseCase từ UseCase khác — dùng Domain Service hoặc sắp xếp qua Application Service
- Tất cả `@Transactional` đặt ở UseCase layer (không ở Repository, không ở Controller)
- Log `[ACTION] Start/Step/Complete | userId=(masked)` level INFO

---

## 5. REPOSITORY RULES

```java
// Interface trong Domain Layer (POJO — không import Spring)
public interface PurchaseRequestRepository {
    void save(PurchaseRequest pr);
    Optional<PurchaseRequest> findById(UUID id);
    Optional<PurchaseRequest> findByPrNumber(String prNumber);
    Page<PurchaseRequest> findByFilter(PrFilter filter, PageRequest page);
    void update(PurchaseRequest pr);
    void softDelete(UUID id, UUID deletedBy);
}

// Implementation trong Infrastructure Layer
@Repository
@RequiredArgsConstructor
public class PurchaseRequestRepositoryImpl implements PurchaseRequestRepository {

    private static final Logger log = LogManager.getLogger(PurchaseRequestRepositoryImpl.class);

    private final PurchaseRequestMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    public void save(PurchaseRequest pr) {
        log.debug("[REPO] save | id={}", pr.getId());
        PurchaseRequestEntity entity = toEntity(pr);
        mapper.insert(entity);
    }

    private PurchaseRequestEntity toEntity(PurchaseRequest pr) {
        // Dùng ObjectMapper để convert, KHÔNG viết thủ công từng field
        return objectMapper.convertValue(pr, PurchaseRequestEntity.class);
    }
}
```

**Repository Rules:**
- Interface ở Domain, Implementation ở Infrastructure
- Dùng `ObjectMapper` để convert Domain ↔ Entity (tránh boilerplate mapping thủ công)
- Log `[REPO] operation | id=xxx` level DEBUG
- Không bao giờ return `null` — dùng `Optional<T>`

---

## 6. EXCEPTION HANDLING

### 6.1 Exception Hierarchy

```java
// Base
public abstract class BusinessException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;

    public BusinessException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}

// Domain exceptions
public class PrNotFoundException extends BusinessException {
    public PrNotFoundException(String prNumber) {
        super("PR_001", "Không tìm thấy yêu cầu mua sắm: " + prNumber, HttpStatus.NOT_FOUND);
    }
}

public class InsufficientBudgetException extends BusinessException {
    public InsufficientBudgetException(Money available, Money required) {
        super("PR_002", String.format("Ngân sách không đủ. Có: %s, Cần: %s",
              available.getAmount(), required.getAmount()), HttpStatus.UNPROCESSABLE_ENTITY);
    }
}

public class ApprovalConflictException extends BusinessException {
    public ApprovalConflictException(String message) {
        super("APR_001", message, HttpStatus.CONFLICT);
    }
}
```

### 6.2 Error Code Convention

```
Format: {SERVICE_PREFIX}_{3-digit-number}

PR_001  — PR không tồn tại
PR_002  — Ngân sách không đủ
PR_003  — PR không thể submit ở trạng thái hiện tại
PR_004  — Requester không được tự duyệt PR của mình
PR_010  — Số lượng phải lớn hơn 0
PR_011  — Đơn giá phải >= 0
PR_020  — Vượt giới hạn EMERGENCY phòng ban (3 lần/tháng)

APR_001 — Xung đột lợi ích phát hiện
APR_002 — Approver không đúng bước
APR_003 — Task đã được xử lý (idempotency)
APR_010 — Comment bắt buộc khi Từ chối

IAM_001 — Thông tin đăng nhập sai
IAM_002 — Tài khoản bị khoá
IAM_003 — Token không hợp lệ hoặc hết hạn
IAM_004 — Không có quyền thực hiện thao tác này
IAM_005 — Phiên đăng nhập đã bị invalidate
IAM_010 — Email chưa được xác thực
IAM_011 — 2FA code sai

FIN_001 — Ngân sách không tồn tại
FIN_002 — 3-Way match thất bại
FIN_010 — Không thể duyệt vượt > 30% ngân sách (cần CEO)

GW_001  — Rate limit vượt mức
GW_002  — x-api-key không hợp lệ
GW_003  — Service không khả dụng (circuit breaker open)
```

### 6.3 Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        log.warn("[EXCEPTION][{}] {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> new FieldError(e.getField(), e.getDefaultMessage()))
                .toList();
        log.warn("[EXCEPTION][VALIDATION] {} field errors", errors.size());
        return ResponseEntity.badRequest()
                .body(ApiResponse.validationError("VAL_001", "Dữ liệu không hợp lệ", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("[EXCEPTION][SYS_001] Unexpected error", ex);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("SYS_001", "Lỗi hệ thống, vui lòng thử lại sau"));
    }
}
```

---

## 7. LOG RULES

### 7.1 Log Layers

```java
// FILTER layer — HTTP request/response
log.info("[REQUEST] {} {} | ip={} | [RESPONSE] {} {}ms", method, url, ip, status, elapsed);

// CONTROLLER layer
log.info("[CONTROLLER] {} {} | userId={}", method, endpoint, userId);

// SERVICE/USE CASE layer
log.info("[ACTION] Start CreatePR | userId={}", maskedUserId);
log.info("[ACTION] Step budget-check | prId={} | available={}", prId, available);
log.info("[ACTION] Complete CreatePR | prNumber={}", prNumber);

// REPOSITORY layer
log.debug("[REPO] insert purchase_request | id={}", id);
log.debug("[REPO] select by id | id={}", id);

// CACHE layer
log.debug("[CACHE] hit | key=budget:{}", key);
log.debug("[CACHE] miss | key=role-perm:{}", roleCode);
log.debug("[CACHE] put | key=session:{} | ttl={}s", token, ttl);

// SECURITY layer
log.warn("[TOKEN] Invalid token | ip={}", ip);
log.warn("[APIKEY] Missing x-api-key | service={}", serviceName);
log.warn("[SECURITY] Unauthorized access | userId={} | permission={}", userId, permission);

// AUDIT layer (separate file, immutable)
log.info("[AUDIT] {} | actor={} | entity={}/{} | result={}", action, actorId, entityType, entityId, result);
```

### 7.2 Masking Rules

```java
public class LogMaskingUtil {
    // email: nguyen.van.a@company.com → n***@c***.com
    public static String maskEmail(String email) { ... }

    // phone: 0912345678 → 09*****678
    public static String maskPhone(String phone) { ... }

    // userId: chỉ show 8 ký tự đầu
    public static String maskUserId(UUID userId) {
        return userId.toString().substring(0, 8) + "...";
    }
}

// KHÔNG bao giờ log:
// - password, newPassword, confirmPassword
// - token (session token)
// - totpSecret
// - encryptedPayload, encryptedAesKey
// - private key
```

### 7.3 Log4j2 Configuration

```xml
<!-- log4j2.xml -->
<Configuration status="WARN" monitorInterval="30">
    <Properties>
        <Property name="LOG_PATTERN">
            %highlight{%d{HH:mm:ss.SSS}}{FATAL=red bold, ERROR=red, WARN=yellow, INFO=green, DEBUG=cyan}
            %style{[%t]}{bright,black} %-5level %cyan{[%logger{1}]}
            %style{traceId=%X{traceId}}{bright,blue} - %msg%n
        </Property>
        <Property name="JSON_PATTERN">{"timestamp":"%d{ISO8601}","level":"%level","service":"${SERVICE_NAME}","traceId":"%X{traceId}","logger":"%logger","message":"%enc{%msg}{JSON}"}%n</Property>
    </Properties>

    <Appenders>
        <!-- Dev console: màu sắc -->
        <Console name="CONSOLE" target="SYSTEM_OUT">
            <PatternLayout pattern="${LOG_PATTERN}"/>
        </Console>

        <!-- Prod: JSON cho Loki -->
        <RollingFile name="APP_JSON" fileName="logs/application.log">
            <PatternLayout pattern="${JSON_PATTERN}"/>
            <Policies><TimeBasedTriggeringPolicy/></Policies>
        </RollingFile>

        <!-- Audit: append-only, riêng biệt -->
        <File name="AUDIT" fileName="logs/audit.log" append="true">
            <PatternLayout pattern="${JSON_PATTERN}"/>
        </File>
    </Appenders>

    <Loggers>
        <Logger name="com.eprocure" level="DEBUG" additivity="false">
            <AppenderRef ref="CONSOLE"/>
            <AppenderRef ref="APP_JSON"/>
        </Logger>
        <Logger name="com.eprocure.audit" level="INFO" additivity="false">
            <AppenderRef ref="AUDIT"/>
        </Logger>
        <!-- Tắt verbose log của framework -->
        <Logger name="org.springframework" level="WARN"/>
        <Logger name="org.mybatis" level="WARN"/>
        <Root level="INFO">
            <AppenderRef ref="CONSOLE"/>
        </Root>
    </Loggers>
</Configuration>
```

---

## 8. SECURITY RULES

### 8.1 @PreAuthorize

```java
// ✅ ĐÚNG — dùng permission code
@PreAuthorize("hasAuthority('PR_APPROVE_L1')")

// ❌ SAI — hardcode role
@PreAuthorize("hasRole('MANAGER')")
@PreAuthorize("hasAnyRole('MANAGER', 'DIRECTOR')")

// ✅ Kết hợp nhiều quyền
@PreAuthorize("hasAuthority('PR_APPROVE_L1') or hasAuthority('PR_APPROVE_L2')")

// ✅ Kết hợp với ownership check
@PreAuthorize("hasAuthority('PR_EDIT_OWN_DRAFT') and @prSecurityService.isOwner(#id, authentication)")
```

### 8.2 Encryption Interceptor

```java
@Component
@ConditionalOnProperty(name = "app.encryption.enabled", havingValue = "true")
public class EncryptionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        // Giải mã request body
        EncryptedPayload payload = extractEncryptedPayload(request);
        String aesKey = rsaService.decrypt(payload.getEncryptedAesKey());
        String plainBody = aesService.decrypt(payload.getEncryptedPayload(), aesKey);
        // Wrap lại request với plain body
        wrapRequest(request, plainBody);
        return true;
    }

    @Override
    public void postHandle(HttpServletResponse response, ...) {
        // Mã hoá response
    }
}
```

### 8.3 x-api-key (Inter-service)

```java
// Gateway gắn x-api-key vào header trước khi forward cho downstream
// Downstream service validate
@Component
public class ApiKeyFilter extends OncePerRequestFilter {
    @Value("${app.api-key}")
    private String expectedApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String apiKey = request.getHeader("X-Api-Key");
        if (!expectedApiKey.equals(apiKey)) {
            log.warn("[APIKEY] Invalid | ip={} | path={}", request.getRemoteAddr(), request.getRequestURI());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid API Key");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
```

---

## 9. TRANSACTION RULES

```java
// ✅ Đặt @Transactional ở UseCase, không ở Repository, không ở Controller
@Transactional
public PurchaseRequest execute(SubmitPrCommand command) { ... }

// ✅ Read-only optimization
@Transactional(readOnly = true)
public Page<PurchaseRequest> findAll(PrFilter filter, PageRequest page) { ... }

// ✅ Propagation cho nested
@Transactional(propagation = Propagation.REQUIRES_NEW)  // Audit log — independent transaction
public void saveAuditLog(AuditLog log) { ... }

// Custom Transaction Manager nếu cần (Kafka transaction, etc.)
@Transactional(transactionManager = "kafkaTransactionManager")
public void publishAndSave(...) { ... }
```

---

## 10. MYBATIS RULES

```java
// Mapper Interface
@Mapper
public interface PurchaseRequestMapper {

    @Insert("""
        INSERT INTO pr.purchase_requests (id, pr_number, requester_id, ...)
        VALUES (#{id}, #{prNumber}, #{requesterId}, ...)
        """)
    void insert(PurchaseRequestEntity entity);

    @Select("""
        SELECT * FROM pr.purchase_requests
        WHERE id = #{id} AND is_deleted = false
        """)
    @Results(id = "prResult", value = {
        @Result(property = "prNumber", column = "pr_number"),
        @Result(property = "requesterId", column = "requester_id"),
        @Result(property = "lineItems", column = "id",
                many = @Many(select = "selectLineItemsByPrId"))
    })
    Optional<PurchaseRequestEntity> findById(UUID id);
}

// XML cho query phức tạp: resources/mapper/PurchaseRequestMapper.xml
```

**MyBatis Rules:**
- Simple CRUD: annotation trong Mapper interface
- Complex query (dynamic WHERE, JOIN nhiều bảng, aggregation): XML mapper
- Result mapping: luôn dùng `@Results` / `<resultMap>` — không dựa vào auto-mapping
- Không viết SQL trong UseCase hay Repository Implementation — chỉ trong Mapper

---

## 11. OBJECT MAPPER USAGE

```java
// Cấu hình ObjectMapper trong Config
@Bean
public ObjectMapper objectMapper() {
    return new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);
}

// Convert Domain ↔ Entity (trong Repository)
PurchaseRequestEntity entity = objectMapper.convertValue(domainObj, PurchaseRequestEntity.class);
PurchaseRequest domain = objectMapper.convertValue(entity, PurchaseRequest.class);

// Convert Request ↔ Command (trong Presentation Mapper)
CreatePrCommand command = objectMapper.convertValue(request, CreatePrCommand.class);
```

---

## 12. SPRING PROFILES & ENVIRONMENT VARIABLES

```yaml
# application.yml — chỉ chứa cấu hình chung, không sensitive
app:
  name: pr-service
  encryption:
    enabled: ${ENCRYPTION_ENABLED:true}
  timezone: ${TZ:Asia/Ho_Chi_Minh}

# application-dev.yml
logging:
  level:
    com.eprocure: DEBUG
app:
  encryption:
    enabled: false

# application-prod.yml
logging:
  level:
    com.eprocure: INFO
    root: WARN
```

**Environment Variable Naming:**
```
DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASS
REDIS_HOST, REDIS_PORT, REDIS_PASS
KAFKA_BOOTSTRAP_SERVERS
KEYCLOAK_URL, KEYCLOAK_REALM, KEYCLOAK_CLIENT_ID, KEYCLOAK_CLIENT_SECRET
ENCRYPTION_ENABLED, ENCRYPTION_RSA_PRIVATE_KEY
API_KEY (x-api-key cho inter-service)
JWT_SECRET (nếu cần)
TZ=Asia/Ho_Chi_Minh
```

---

## 13. DOCKER RESOURCE LIMITS

```yaml
# docker-compose.yml snippet
services:
  pr-service:
    image: eprocure/pr-service:latest
    environment:
      - TZ=Asia/Ho_Chi_Minh
      - SPRING_PROFILES_ACTIVE=dev
    deploy:
      resources:
        limits:
          cpus: '0.25'
          memory: 512M
        reservations:
          cpus: '0.1'
          memory: 256M
```

**Resource Budget (8GB RAM machine):**

| Service | CPU Limit | RAM Limit |
|---|---|---|
| API Gateway (NGINX) | 0.25 | 128MB |
| IAM Service | 0.25 | 512MB |
| PR Service | 0.25 | 512MB |
| Approval Engine (Camunda) | 0.5 | 768MB |
| Finance Service | 0.25 | 512MB |
| Inventory Service | 0.25 | 256MB |
| Notification Service | 0.1 | 256MB |
| PostgreSQL (all DBs) | 0.5 | 512MB |
| Redis | 0.1 | 256MB |
| Kafka + Zookeeper | 0.5 | 512MB |
| Keycloak | 0.25 | 512MB |
| Prometheus | 0.1 | 256MB |
| Grafana + Loki + Tempo | 0.25 | 256MB |
| **TOTAL** | **~3.5** | **~5.3GB** |

---

## 14. UNIT TEST RULES

```java
@ExtendWith(MockitoExtension.class)
class CreatePurchaseRequestUseCaseTest {

    @Mock PurchaseRequestRepository repository;
    @Mock IdempotencyService idempotencyService;
    @Mock PrNumberGenerator prNumberGenerator;

    @InjectMocks CreatePurchaseRequestUseCase useCase;

    @Test
    @DisplayName("Tạo PR thành công với dữ liệu hợp lệ")
    void should_create_pr_when_valid_command() {
        // Given
        CreatePrCommand command = TestDataFactory.validCreatePrCommand();
        given(idempotencyService.get(any(), any())).willReturn(Optional.empty());
        given(prNumberGenerator.next()).willReturn("PR-2025-01-00001");

        // When
        PurchaseRequest result = useCase.execute(command, "idempotency-key-123");

        // Then
        assertThat(result.getPrNumber()).isEqualTo("PR-2025-01-00001");
        assertThat(result.getStatus()).isEqualTo(PrStatus.DRAFT);
        verify(repository).save(any(PurchaseRequest.class));
    }

    @Test
    @DisplayName("Ném ValidationException khi không có line items")
    void should_throw_when_no_line_items() {
        CreatePrCommand command = TestDataFactory.commandWithNoLineItems();

        assertThatThrownBy(() -> useCase.execute(command, "key"))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("PR phải có ít nhất 1 mục");
    }
}
```

**Test Rules:**
- Test method name: `should_{expected}_when_{condition}`
- Dùng `@DisplayName` với tiếng Việt để dễ đọc
- Mỗi test: Given / When / Then rõ ràng
- Coverage target: ≥ 70% cho package `application` và `domain`
- Không test private methods trực tiếp
- Test data factory: `TestDataFactory` class riêng trong test package

---

## 15. CODE REVIEW CHECKLIST

```
✅ Không có hardcode role trong @PreAuthorize
✅ Không có hardcode string tiếng Việt visible trong Angular template (dùng translate pipe)
✅ Không có hardcode màu trong Angular component (dùng CSS variable)
✅ Không có hardcode URL, credential trong Java code (dùng @Value hoặc env var)
✅ Không có HTTP DELETE endpoint
✅ Tất cả soft delete có is_deleted + deleted_at + deleted_by
✅ Không có float/double cho tiền tệ (dùng BigDecimal/NUMERIC)
✅ Log không chứa password, token, private key
✅ @Transactional đặt đúng layer (UseCase, không phải Repository)
✅ ObjectMapper được dùng cho conversion thay vì manual mapping
✅ Exception có error code theo convention
✅ Mọi public API method có JavaDoc
✅ Index đúng cho query patterns
✅ Unit test cover happy path + main error paths
✅ Idempotency header được handle
✅ Docker resource limits được set
```
