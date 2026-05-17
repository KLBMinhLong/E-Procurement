# coding-patterns.md
## eProcure Enterprise — Code Patterns Tái Sử Dụng

> Copy-paste patterns đã được verify theo CODING_RULES.  
> Dùng làm template khi tạo class mới — KHÔNG tự nghĩ lại từ đầu.

---

## 1. USE CASE PATTERN

```java
package com.eprocure.{service}.application.usecase;

/**
 * Use Case: [Mô tả hành động nghiệp vụ].
 *
 * <p>Flow:
 * <ol>
 *   <li>Check idempotency</li>
 *   <li>Validate business rules</li>
 *   <li>Execute core logic</li>
 *   <li>Save domain object</li>
 *   <li>Publish domain event (nếu cần)</li>
 *   <li>Cache idempotency result</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class {Verb}{Noun}UseCase {

    private static final Logger log = LogManager.getLogger({Verb}{Noun}UseCase.class);

    private final {Entity}Repository {entity}Repository;
    private final IdempotencyService idempotencyService;
    // thêm dependencies cần thiết

    @Transactional
    public {Entity} execute({Verb}{Noun}Command command, String idempotencyKey) {
        // 1. Idempotency check
        Optional<{Entity}> cached = idempotencyService.check(idempotencyKey, {Entity}.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit | key={}", idempotencyKey.substring(0, 8));
            return cached.get();
        }

        log.info("[ACTION] Start {Verb}{Noun} | userId={}", LogMaskingUtil.maskId(command.getActorId()));

        // 2. Validate
        validate(command);

        // 3. Build domain object
        {Entity} entity = {Entity}.create(command);

        // 4. Save
        {entity}Repository.save(entity);

        // 5. Publish event (optional)
        // eventPublisher.publish(new {Entity}CreatedEvent(entity));

        // 6. Cache idempotency
        idempotencyService.save(idempotencyKey, entity);

        log.info("[ACTION] Complete {Verb}{Noun} | id={}", entity.getId());
        return entity;
    }

    private void validate({Verb}{Noun}Command command) {
        // Business validation — throw BusinessException nếu vi phạm
    }
}
```

---

## 2. REPOSITORY INTERFACE PATTERN (Domain Layer)

```java
package com.eprocure.{service}.domain.repository;

// KHÔNG import Spring/Jakarta — POJO interface thuần
public interface {Entity}Repository {
    void save({Entity} entity);
    void update({Entity} entity);
    Optional<{Entity}> findById(UUID id);
    Optional<{Entity}> findBy{UniqueField}(String value);
    Page<{Entity}> findByFilter({Entity}Filter filter, PageRequest page);
    void softDelete(UUID id, UUID deletedBy);
    boolean existsBy{UniqueField}(String value);
}
```

---

## 3. REPOSITORY IMPL PATTERN (Infrastructure Layer)

```java
package com.eprocure.{service}.infrastructure.persistence.repository;

@Repository
@RequiredArgsConstructor
public class {Entity}RepositoryImpl implements {Entity}Repository {

    private static final Logger log = LogManager.getLogger({Entity}RepositoryImpl.class);

    private final {Entity}Mapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    public void save({Entity} entity) {
        log.debug("[REPO] insert {entity} | id={}", entity.getId());
        mapper.insert(objectMapper.convertValue(entity, {Entity}DbEntity.class));
    }

    @Override
    public void update({Entity} entity) {
        log.debug("[REPO] update {entity} | id={}", entity.getId());
        mapper.update(objectMapper.convertValue(entity, {Entity}DbEntity.class));
    }

    @Override
    public Optional<{Entity}> findById(UUID id) {
        log.debug("[REPO] findById {entity} | id={}", id);
        return mapper.findById(id)
            .map(e -> objectMapper.convertValue(e, {Entity}.class));
    }

    @Override
    public Page<{Entity}> findByFilter({Entity}Filter filter, PageRequest page) {
        log.debug("[REPO] findByFilter {entity} | filter={}", filter);
        List<{Entity}DbEntity> entities = mapper.findByFilter(filter, page);
        long total = entities.isEmpty() ? 0 : entities.get(0).getTotalCount();
        List<{Entity}> domain = entities.stream()
            .map(e -> objectMapper.convertValue(e, {Entity}.class))
            .toList();
        return Page.of(domain, total, page);
    }

    @Override
    public void softDelete(UUID id, UUID deletedBy) {
        log.debug("[REPO] softDelete {entity} | id={}", id);
        mapper.softDelete(id, deletedBy, Instant.now());
    }
}
```

---

## 4. MYBATIS MAPPER PATTERN

```java
package com.eprocure.{service}.infrastructure.persistence.mapper;

@Mapper
public interface {Entity}Mapper {

    @Insert("""
        INSERT INTO {schema}.{table} (
            id, {field1}, {field2}, created_at, updated_at, created_by, is_deleted
        ) VALUES (
            #{id}, #{field1}, #{field2}, NOW(), NOW(), #{createdBy}, false
        )
        """)
    void insert({Entity}DbEntity entity);

    @Update("""
        UPDATE {schema}.{table}
        SET {field1} = #{field1},
            {field2} = #{field2},
            updated_at = NOW()
        WHERE id = #{id} AND is_deleted = false
        """)
    void update({Entity}DbEntity entity);

    @Select("SELECT *, COUNT(*) OVER() AS total_count FROM {schema}.{table} WHERE id = #{id} AND is_deleted = false")
    @Results(id = "{entity}Result", value = {
        @Result(property = "field1", column = "field_1"),
        @Result(property = "field2", column = "field_2"),
        @Result(property = "createdBy", column = "created_by")
    })
    Optional<{Entity}DbEntity> findById(UUID id);

    @Update("""
        UPDATE {schema}.{table}
        SET is_deleted = true, deleted_at = #{deletedAt}, deleted_by = #{deletedBy}
        WHERE id = #{id} AND is_deleted = false
        """)
    void softDelete(@Param("id") UUID id, @Param("deletedBy") UUID deletedBy,
                    @Param("deletedAt") Instant deletedAt);

    // Complex queries → XML mapper
    List<{Entity}DbEntity> findByFilter(@Param("filter") {Entity}Filter filter,
                                        @Param("page") PageRequest page);
}
```

```xml
<!-- resources/mapper/{Entity}Mapper.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.eprocure.{service}.infrastructure.persistence.mapper.{Entity}Mapper">

    <resultMap id="{entity}FilterResult" type="com.eprocure.{service}.infrastructure.persistence.entity.{Entity}DbEntity">
        <id property="id" column="id"/>
        <result property="field1" column="field_1"/>
        <result property="field2" column="field_2"/>
        <result property="totalCount" column="total_count"/>
    </resultMap>

    <select id="findByFilter" resultMap="{entity}FilterResult">
        SELECT
            t.*,
            COUNT(*) OVER() AS total_count
        FROM {schema}.{table} t
        WHERE t.is_deleted = false
        <if test="filter.status != null">
            AND t.status = #{filter.status}
        </if>
        <if test="filter.departmentId != null">
            AND t.department_id = #{filter.departmentId}
        </if>
        <if test="filter.fromDate != null">
            AND t.created_at >= #{filter.fromDate}
        </if>
        <if test="filter.toDate != null">
            AND t.created_at &lt;= #{filter.toDate}
        </if>
        <if test="filter.q != null and filter.q != ''">
            AND (t.title ILIKE '%' || #{filter.q} || '%'
                 OR t.{number_field} ILIKE #{filter.q} || '%')
        </if>
        ORDER BY
        <choose>
            <when test="page.sort != null and page.sort.field == 'createdAt'">t.created_at</when>
            <when test="page.sort != null and page.sort.field == 'totalAmount'">t.total_amount</when>
            <otherwise>t.created_at</otherwise>
        </choose>
        <if test="page.sort != null and page.sort.direction == 'asc'">ASC</if>
        <if test="page.sort == null or page.sort.direction != 'asc'">DESC</if>
        LIMIT #{page.size} OFFSET #{page.offset}
    </select>

</mapper>
```

---

## 5. CONTROLLER PATTERN

```java
@RestController
@RequestMapping("/api/v1/{resources}")
@RequiredArgsConstructor
@Tag(name = "{Entity}", description = "API quản lý {entity}")
public class {Entity}Controller {

    private static final Logger log = LogManager.getLogger({Entity}Controller.class);

    private final {Verb}{Noun}UseCase {verb}{Noun}UseCase;
    private final Get{Entity}ListUseCase getListUseCase;
    private final Get{Entity}DetailUseCase getDetailUseCase;
    private final {Entity}PresentationMapper mapper;

    @GetMapping
    @PreAuthorize("hasAuthority('{RESOURCE}_VIEW_OWN')")
    @Operation(summary = "Danh sách {entity}")
    public ResponseEntity<ApiResponse<List<{Entity}SummaryResponse>>> list(
            @ModelAttribute {Entity}FilterRequest filterRequest,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("[CONTROLLER] GET /{resources} | userId={}", principal.getMaskedId());
        var filter = mapper.toFilter(filterRequest, principal);
        var page = mapper.toPageRequest(filterRequest);
        var result = getListUseCase.execute(filter, page);

        return ResponseEntity.ok(ApiResponse.success("OK", mapper.toSummaryList(result.getContent()),
            PageMeta.from(result)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('{RESOURCE}_VIEW_OWN')")
    public ResponseEntity<ApiResponse<{Entity}DetailResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("[CONTROLLER] GET /{resources}/{} | userId={}", id, principal.getMaskedId());
        var entity = getDetailUseCase.execute(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("OK", mapper.toDetailResponse(entity)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('{RESOURCE}_CREATE')")
    public ResponseEntity<ApiResponse<{Entity}CreatedResponse>> create(
            @Valid @RequestBody Create{Entity}Request request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("[CONTROLLER] POST /{resources} | userId={}", principal.getMaskedId());
        var command = mapper.toCreateCommand(request, principal.getId());
        var entity = {verb}{Noun}UseCase.execute(command, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("{RESOURCE}_CREATED", mapper.toCreatedResponse(entity)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('{RESOURCE}_CANCEL_OWN')")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable UUID id,
            @Valid @RequestBody Cancel{Entity}Request request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("[CONTROLLER] PATCH /{resources}/{}/cancel | userId={}", id, principal.getMaskedId());
        cancel{Entity}UseCase.execute(id, request.getReason(), principal.getId(), idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok("{RESOURCE}_CANCELLED", "{Entity} đã được hủy thành công"));
    }
}
```

---

## 6. DOMAIN ENTITY PATTERN (POJO thuần)

```java
package com.eprocure.{service}.domain.model;

// KHÔNG import Spring, Jakarta, MyBatis
public class {Entity} {

    // Immutable identity
    private final UUID id;

    // Mutable state
    private {Entity}Status status;
    private String someField;
    // ...

    // Private constructor — dùng factory method
    private {Entity}(UUID id, {Entity}Status status, String someField) {
        this.id = id;
        this.status = status;
        this.someField = someField;
    }

    // ── Factory Methods ──────────────────────────────────────
    public static {Entity} create(Create{Entity}Command command) {
        Objects.requireNonNull(command, "command must not be null");
        return new {Entity}(
            UUID.randomUUID(),
            {Entity}Status.DRAFT,
            command.getSomeField()
        );
    }

    // ── Business Methods (Domain Logic) ─────────────────────
    public void submit() {
        if (this.status != {Entity}Status.DRAFT) {
            throw new InvalidStateTransitionException(this.status, "submit");
        }
        this.status = {Entity}Status.SUBMITTED;
    }

    public boolean canBeEditedBy(UUID userId) {
        return this.status == {Entity}Status.DRAFT
            && this.createdBy.equals(userId);
    }

    // ── Getters ONLY — no setters (enforce via methods) ─────
    public UUID getId() { return id; }
    public {Entity}Status getStatus() { return status; }
    public String getSomeField() { return someField; }
}
```

---

## 7. DOMAIN EVENT PATTERN

```java
// Domain event — POJO thuần
public record {Entity}SubmittedEvent(
    String eventId,
    String eventType,
    String version,
    String source,
    Instant timestamp,
    UUID traceId,
    {Entity}SubmittedPayload payload
) {
    public {Entity}SubmittedEvent(UUID entityId, UUID actorId) {
        this(
            UUID.randomUUID().toString(),
            "{ENTITY}_SUBMITTED",
            "1.0",
            "{service-name}",
            Instant.now(),
            extractTraceId(),   // Từ OTel MDC
            new {Entity}SubmittedPayload(entityId, actorId)
        );
    }

    public record {Entity}SubmittedPayload(UUID entityId, UUID actorId) {}
}

// Kafka publisher (Infrastructure)
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish({Entity}SubmittedEvent event) {
        kafkaTemplate.send("{domain}.{entity}.submitted",
            event.payload().entityId().toString(),
            event);
    }
}
```

---

## 8. ERROR CODE ENUM PATTERN (mỗi service)

```java
public enum {Service}ErrorCode {

    // Not Found
    {ENTITY}_NOT_FOUND("{SVC}_001", HttpStatus.NOT_FOUND,
        "{Entity} không tồn tại"),

    // Business Rules
    INVALID_STATE("{SVC}_010", HttpStatus.CONFLICT,
        "Không thể thực hiện thao tác ở trạng thái hiện tại"),

    // Validation
    INVALID_INPUT("{SVC}_020", HttpStatus.BAD_REQUEST,
        "Dữ liệu không hợp lệ");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    {Service}ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() { return code; }
    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getDefaultMessage() { return defaultMessage; }
}
```

---

## 9. FLYWAY MIGRATION TEMPLATE

```sql
-- V{N}__{description}.sql
-- Migration: {Mô tả ngắn về thay đổi}
-- Author: {Tên developer}
-- Date: {YYYY-MM-DD}

CREATE SCHEMA IF NOT EXISTS {schema};

CREATE TABLE {schema}.{table} (
    -- Identity
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Business fields
    {field1}        VARCHAR(50)     NOT NULL,
    {field2}        NUMERIC(19,4)   NOT NULL DEFAULT 0,
    status          VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',

    -- Audit
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by      UUID,

    -- Soft delete
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID,

    -- Constraints
    CONSTRAINT chk_{table}_status CHECK (status IN ('DRAFT','ACTIVE','CLOSED'))
);

-- Comments
COMMENT ON TABLE {schema}.{table} IS '{Mô tả table}';
COMMENT ON COLUMN {schema}.{table}.{field1} IS '{Mô tả column}';

-- Indexes
CREATE INDEX idx_{table}_{field1} ON {schema}.{table}({field1}) WHERE is_deleted = FALSE;
CREATE INDEX idx_{table}_created_at ON {schema}.{table}(created_at DESC) WHERE is_deleted = FALSE;

-- Auto-update trigger
CREATE TRIGGER trg_{table}_updated_at
    BEFORE UPDATE ON {schema}.{table}
    FOR EACH ROW
    EXECUTE FUNCTION {schema}.update_updated_at();
```

---

## 10. ANGULAR SERVICE PATTERN

```typescript
@Injectable({ providedIn: 'root' })
export class {Entity}Service {
    private readonly api = inject(ApiService);

    private readonly BASE = '/{entities}';

    list(filter: {Entity}Filter): Observable<ApiResponse<{Entity}Summary[]>> {
        const params = this.buildParams(filter);
        return this.api.get<{Entity}Summary[]>(this.BASE, params);
    }

    getById(id: string): Observable<ApiResponse<{Entity}Detail>> {
        return this.api.get<{Entity}Detail>(`${this.BASE}/${id}`);
    }

    create(request: Create{Entity}Request): Observable<ApiResponse<{Entity}Created>> {
        return this.api.post<{Entity}Created>(this.BASE, request);
    }

    cancel(id: string, reason: string): Observable<ApiResponse<void>> {
        return this.api.patch<void>(`${this.BASE}/${id}/cancel`, { reason });
    }

    private buildParams(filter: {Entity}Filter): Record<string, string> {
        const params: Record<string, string> = {
            page: String(filter.page ?? 1),
            size: String(filter.size ?? 20),
            sort: filter.sort ?? 'createdAt,desc'
        };
        if (filter.status)       params['status']       = filter.status;
        if (filter.departmentId) params['department_id'] = filter.departmentId;
        if (filter.q)            params['q']            = filter.q;
        return params;
    }
}
```
