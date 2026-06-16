package com.eprocure.admin.presentation.controller;

import com.eprocure.admin.application.usecase.CreateDepartmentUseCase;
import com.eprocure.admin.application.usecase.DeactivateDepartmentUseCase;
import com.eprocure.admin.application.usecase.UpdateDepartmentUseCase;
import com.eprocure.admin.common.api.ApiResponse;
import com.eprocure.admin.common.api.RequestIdUtil;
import com.eprocure.admin.common.security.UserPrincipal;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.presentation.mapper.AdminDepartmentPresentationMapper;
import com.eprocure.admin.presentation.request.DepartmentRequest;
import com.eprocure.admin.presentation.response.DepartmentResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/departments")
public class AdminDepartmentController {
    private static final Logger log = LogManager.getLogger(AdminDepartmentController.class);

    private final CreateDepartmentUseCase createDepartmentUseCase;
    private final UpdateDepartmentUseCase updateDepartmentUseCase;
    private final DeactivateDepartmentUseCase deactivateDepartmentUseCase;
    private final AdminDepartmentPresentationMapper mapper;

    public AdminDepartmentController(
            CreateDepartmentUseCase createDepartmentUseCase,
            UpdateDepartmentUseCase updateDepartmentUseCase,
            DeactivateDepartmentUseCase deactivateDepartmentUseCase,
            AdminDepartmentPresentationMapper mapper) {
        this.createDepartmentUseCase = createDepartmentUseCase;
        this.updateDepartmentUseCase = updateDepartmentUseCase;
        this.deactivateDepartmentUseCase = deactivateDepartmentUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN_DEPARTMENT_MANAGE')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody DepartmentRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/admin/departments | userId={} | code={}",
                LogMaskingUtil.maskId(principal.getId()),
                body.code());
        String requestId = RequestIdUtil.resolve(request);
        var view = createDepartmentUseCase.execute(
                mapper.toCreateCommand(body, principal, mapper.toAuditContext(principal, request, requestId)),
                idempotencyKey);
        return ResponseEntity.created(URI.create("/api/v1/admin/departments/" + view.id()))
                .body(ApiResponse.success(mapper.toResponse(view), requestId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN_DEPARTMENT_MANAGE')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(
            @PathVariable UUID id,
            @Valid @RequestBody DepartmentRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PUT /api/v1/admin/departments/{} | userId={}",
                LogMaskingUtil.maskId(id),
                LogMaskingUtil.maskId(principal.getId()));
        String requestId = RequestIdUtil.resolve(request);
        var view = updateDepartmentUseCase.execute(
                mapper.toUpdateCommand(id, body, principal, mapper.toAuditContext(principal, request, requestId)),
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(view), requestId));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('ADMIN_DEPARTMENT_MANAGE')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> deactivateDepartment(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PATCH /api/v1/admin/departments/{}/deactivate | userId={}",
                LogMaskingUtil.maskId(id),
                LogMaskingUtil.maskId(principal.getId()));
        String requestId = RequestIdUtil.resolve(request);
        var view = deactivateDepartmentUseCase.execute(
                mapper.toDeactivateCommand(id, principal, mapper.toAuditContext(principal, request, requestId)),
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(view), requestId));
    }
}
