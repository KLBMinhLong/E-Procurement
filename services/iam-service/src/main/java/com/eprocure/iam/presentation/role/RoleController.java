package com.eprocure.iam.presentation.role;

import com.eprocure.iam.application.port.in.CreateRoleCommand;
import com.eprocure.iam.application.port.in.UpdateRolePermissionsCommand;
import com.eprocure.iam.application.service.PermissionView;
import com.eprocure.iam.application.service.RoleDetailView;
import com.eprocure.iam.application.usecase.CreateRoleUseCase;
import com.eprocure.iam.application.usecase.ListPermissionsUseCase;
import com.eprocure.iam.application.usecase.ListRolesUseCase;
import com.eprocure.iam.application.usecase.UpdateRolePermissionsUseCase;
import com.eprocure.iam.common.api.ApiResponse;
import com.eprocure.iam.common.api.RequestIdUtil;
import com.eprocure.iam.common.security.UserPrincipal;
import com.eprocure.iam.common.util.LogMaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RoleController {
    private static final Logger log = LogManager.getLogger(RoleController.class);
    private final ListRolesUseCase listRolesUseCase;
    private final CreateRoleUseCase createRoleUseCase;
    private final UpdateRolePermissionsUseCase updateRolePermissionsUseCase;
    private final ListPermissionsUseCase listPermissionsUseCase;

    public RoleController(
            ListRolesUseCase listRolesUseCase,
            CreateRoleUseCase createRoleUseCase,
            UpdateRolePermissionsUseCase updateRolePermissionsUseCase,
            ListPermissionsUseCase listPermissionsUseCase) {
        this.listRolesUseCase = listRolesUseCase;
        this.createRoleUseCase = createRoleUseCase;
        this.updateRolePermissionsUseCase = updateRolePermissionsUseCase;
        this.listPermissionsUseCase = listPermissionsUseCase;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ADMIN_ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<List<RoleDetailView>>> listRoles(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/roles | userId={}", LogMaskingUtil.maskId(principal.getId()));
        return ResponseEntity.ok(ApiResponse.success(listRolesUseCase.execute(), RequestIdUtil.resolve(request)));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ADMIN_ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<RoleDetailView>> createRole(
            @Valid @RequestBody CreateRoleRequest body,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/roles | userId={}", LogMaskingUtil.maskId(principal.getId()));
        RoleDetailView view = createRoleUseCase.execute(
                new CreateRoleCommand(principal.getId(), body.code(), body.name(), body.description(), body.permissions()),
                idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(view, RequestIdUtil.resolve(request)));
    }

    @PutMapping("/roles/{code}/permissions")
    @PreAuthorize("hasAuthority('ADMIN_ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> updateRolePermissions(
            @PathVariable String code,
            @Valid @RequestBody UpdateRolePermissionsRequest body,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PUT /api/v1/roles/{}/permissions | userId={}",
                code,
                LogMaskingUtil.maskId(principal.getId()));
        updateRolePermissionsUseCase.execute(
                new UpdateRolePermissionsCommand(principal.getId(), code, body.permissions()),
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.successMessage("Role permissions updated", RequestIdUtil.resolve(request)));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ADMIN_ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, List<PermissionView>>>> listPermissions(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/permissions | userId={}", LogMaskingUtil.maskId(principal.getId()));
        return ResponseEntity.ok(ApiResponse.success(listPermissionsUseCase.execute(), RequestIdUtil.resolve(request)));
    }
}
