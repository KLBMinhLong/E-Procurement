## SK-04 · REST Controller Generation

### Trigger
Agent tạo REST endpoint mới cho một feature.

### Inputs Required
- Resource path (hyphen-case)
- Request/Response DTO
- Permission codes
- Idempotency-Key header usage

### Rules
```
[R1] KHÔNG có HTTP DELETE endpoint — dùng PATCH /cancel, /deactivate, /archive
[R2] Controller KHÔNG chứa business logic — chỉ delegate xuống Use Case
[R3] Mọi endpoint bảo vệ bởi @PreAuthorize với permission code, KHÔNG phải role
[R4] Request validation bằng @Valid + Bean Validation annotations
[R5] Response luôn trả về ApiResponse<T> (success, code, message, data, meta, timestamp, requestId)
[R6] API versioning: /api/v1/...
[R7] Endpoints sử dụng hyphen-case: /purchase-requests, không phải /purchaseRequests
[R8] Action endpoints: POST /purchase-requests/{id}/submit, /approve, /cancel
[R9] Idempotency-Key header được đọc và forward xuống Use Case
[R10] Pagination: GET với query params page, size, sort
```

### Template — Controller
```java
package com.eprocure.{service}.presentation.controller;

import com.eprocure.{service}.application.usecase.*;
import com.eprocure.{service}.common.security.CurrentUser;
import com.eprocure.{service}.presentation.mapper.{Entity}PresentationMapper;
import com.eprocure.{service}.presentation.request.*;
import com.eprocure.{service}.presentation.response.*;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller: {Entity}Controller
 * REST API for {EntityName} operations.
 */
@RestController
@RequestMapping("/api/v1/{resource-path}")
public class {Entity}Controller {

    private static final Logger log = LogManager.getLogger({Entity}Controller.class);

    private final Create{Entity}UseCase createUseCase;
    private final Submit{Entity}UseCase submitUseCase;
    private final Get{Entity}UseCase getUseCase;
    private final {Entity}PresentationMapper presentationMapper;

    // Constructor injection only
    public {Entity}Controller(
            Create{Entity}UseCase createUseCase,
            Submit{Entity}UseCase submitUseCase,
            Get{Entity}UseCase getUseCase,
            {Entity}PresentationMapper presentationMapper) {
        this.createUseCase = createUseCase;
        this.submitUseCase = submitUseCase;
        this.getUseCase = getUseCase;
        this.presentationMapper = presentationMapper;
    }

    /**
     * Tạo {entity} mới.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('{ENTITY}_CREATE')")
    public ResponseEntity<ApiResponse<{Entity}Response>> create(
            @Valid @RequestBody Create{Entity}Request request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @CurrentUser UserContext currentUser) {

        log.info("[CONTROLLER] POST /api/v1/{resource-path} | actor={}", currentUser.getUserId());

        var command = presentationMapper.toCreateCommand(request, currentUser.getUserId());
        var result  = createUseCase.execute(command, idempotencyKey);
        var response = presentationMapper.toResponse(result);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("{ENTITY}_CREATED", response, null));
    }

    /**
     * Lấy danh sách {entity} có phân trang.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('{ENTITY}_VIEW_LIST')")
        public ResponseEntity<ApiResponse<List<{Entity}Response>>> getList(
            @ModelAttribute {Entity}FilterRequest filterRequest,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser UserContext currentUser) {

        log.info("[CONTROLLER] GET /api/v1/{resource-path} | actor={} | page={} | size={}",
                currentUser.getUserId(), page, size);

        var filter = presentationMapper.toFilter(filterRequest);
        var result = getUseCase.findAll(filter, page, size);
        var data = result.items().stream().map(presentationMapper::toResponse).toList();

        return ResponseEntity.ok(ApiResponse.success(
            "OK", data, result.meta()));
    }

    /**
     * Submit {entity} để phê duyệt.
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('{ENTITY}_SUBMIT')")
    public ResponseEntity<ApiResponse<{Entity}Response>> submit(
            @PathVariable("id") UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @CurrentUser UserContext currentUser) {

        log.info("[CONTROLLER] POST /api/v1/{resource-path}/{}/submit | actor={}", id, currentUser.getUserId());

        var command = new Submit{Entity}Command(currentUser.getUserId(), id);
        var result  = submitUseCase.execute(command, idempotencyKey);

        return ResponseEntity.ok(ApiResponse.success(
            "{ENTITY}_SUBMITTED", presentationMapper.toResponse(result), null));
    }

    /**
     * Hủy {entity} (soft delete action — NOT DELETE HTTP method).
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('{ENTITY}_CANCEL_OWN') or hasAuthority('{ENTITY}_CANCEL_ANY')")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable("id") UUID id,
            @Valid @RequestBody Cancel{Entity}Request request,
            @CurrentUser UserContext currentUser) {

        log.info("[CONTROLLER] PATCH /api/v1/{resource-path}/{}/cancel | actor={}", id, currentUser.getUserId());

        var command = new Cancel{Entity}Command(currentUser.getUserId(), id, request.getReason());
        cancelUseCase.execute(command, UUID.randomUUID().toString());

        return ResponseEntity.ok(ApiResponse.success("{ENTITY}_CANCELLED", null, null));
    }
}
```

### Template — ApiResponse
```java
public record ApiResponse<T>(
    boolean success,
    String code,
    String message,
    T data,
    Object meta,
    Instant timestamp,
    String requestId
) {
    public static <T> ApiResponse<T> success(String code, T data, Object meta) {
        return new ApiResponse<>(true, code, null, data, meta,
            Instant.now(), MDC.get("requestId"));
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null, null,
            Instant.now(), MDC.get("requestId"));
    }
}
```

### Checklist
```
[ ] Không có HTTP DELETE endpoint
[ ] @PreAuthorize dùng permission code
[ ] ApiResponse được dùng cho response
[ ] Idempotency-Key được forward xuống Use Case
```
