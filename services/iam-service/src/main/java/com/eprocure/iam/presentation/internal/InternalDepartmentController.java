package com.eprocure.iam.presentation.internal;

import com.eprocure.iam.application.port.in.DeactivateDepartmentCommand;
import com.eprocure.iam.application.port.in.ManageDepartmentCommand;
import com.eprocure.iam.application.service.DepartmentAdminView;
import com.eprocure.iam.application.service.InternalApiKeyGuard;
import com.eprocure.iam.application.usecase.CreateDepartmentUseCase;
import com.eprocure.iam.application.usecase.DeactivateDepartmentUseCase;
import com.eprocure.iam.application.usecase.UpdateDepartmentUseCase;
import com.eprocure.iam.common.api.ApiResponse;
import com.eprocure.iam.common.api.RequestIdUtil;
import com.eprocure.iam.common.util.LogMaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/org/departments")
public class InternalDepartmentController {
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final Logger log = LogManager.getLogger(InternalDepartmentController.class);

    private final InternalApiKeyGuard internalApiKeyGuard;
    private final CreateDepartmentUseCase createDepartmentUseCase;
    private final UpdateDepartmentUseCase updateDepartmentUseCase;
    private final DeactivateDepartmentUseCase deactivateDepartmentUseCase;

    public InternalDepartmentController(
            InternalApiKeyGuard internalApiKeyGuard,
            CreateDepartmentUseCase createDepartmentUseCase,
            UpdateDepartmentUseCase updateDepartmentUseCase,
            DeactivateDepartmentUseCase deactivateDepartmentUseCase) {
        this.internalApiKeyGuard = internalApiKeyGuard;
        this.createDepartmentUseCase = createDepartmentUseCase;
        this.updateDepartmentUseCase = updateDepartmentUseCase;
        this.deactivateDepartmentUseCase = deactivateDepartmentUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentAdminView>> createDepartment(
            @RequestHeader(INTERNAL_API_KEY_HEADER) String internalApiKey,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody InternalDepartmentRequest body,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] POST /internal/org/departments | userId=internal | actorId={} | code={}",
                LogMaskingUtil.maskId(body.actorId()),
                body.code());
        DepartmentAdminView view = createDepartmentUseCase.execute(toCreateCommand(body), idempotencyKey);
        return ResponseEntity.created(URI.create("/internal/org/departments/" + view.id()))
                .body(ApiResponse.success(view, RequestIdUtil.resolve(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentAdminView>> updateDepartment(
            @RequestHeader(INTERNAL_API_KEY_HEADER) String internalApiKey,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable UUID id,
            @Valid @RequestBody InternalDepartmentRequest body,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] PUT /internal/org/departments/{} | userId=internal | actorId={}",
                LogMaskingUtil.maskId(id),
                LogMaskingUtil.maskId(body.actorId()));
        DepartmentAdminView view = updateDepartmentUseCase.execute(toUpdateCommand(id, body), idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(view, RequestIdUtil.resolve(request)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<DepartmentAdminView>> deactivateDepartment(
            @RequestHeader(INTERNAL_API_KEY_HEADER) String internalApiKey,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable UUID id,
            @Valid @RequestBody InternalDepartmentDeactivateRequest body,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] PATCH /internal/org/departments/{}/deactivate | userId=internal | actorId={}",
                LogMaskingUtil.maskId(id),
                LogMaskingUtil.maskId(body.actorId()));
        DepartmentAdminView view = deactivateDepartmentUseCase.execute(
                new DeactivateDepartmentCommand(body.actorId(), id),
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(view, RequestIdUtil.resolve(request)));
    }

    private ManageDepartmentCommand toCreateCommand(InternalDepartmentRequest body) {
        return new ManageDepartmentCommand(
                body.actorId(),
                null,
                body.code(),
                body.name(),
                body.parentId(),
                body.headUserId());
    }

    private ManageDepartmentCommand toUpdateCommand(UUID id, InternalDepartmentRequest body) {
        return new ManageDepartmentCommand(
                body.actorId(),
                id,
                body.code(),
                body.name(),
                body.parentId(),
                body.headUserId());
    }
}
