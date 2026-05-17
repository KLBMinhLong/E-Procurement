package com.eprocure.iam.presentation.user;

import com.eprocure.iam.application.port.in.AssignUserRolesCommand;
import com.eprocure.iam.application.port.in.ChangeUserStatusCommand;
import com.eprocure.iam.application.port.in.ConfirmTwoFactorCommand;
import com.eprocure.iam.application.port.in.CreateUserCommand;
import com.eprocure.iam.application.port.in.EnableTwoFactorCommand;
import com.eprocure.iam.application.port.in.ListUsersQuery;
import com.eprocure.iam.application.port.in.UpdateUserCommand;
import com.eprocure.iam.application.service.CurrentUserView;
import com.eprocure.iam.application.service.PageResult;
import com.eprocure.iam.application.service.TwoFactorConfirmView;
import com.eprocure.iam.application.service.TwoFactorSetupView;
import com.eprocure.iam.application.service.UserDetailView;
import com.eprocure.iam.application.service.UserSummaryView;
import com.eprocure.iam.application.usecase.AssignUserRolesUseCase;
import com.eprocure.iam.application.usecase.ChangeUserStatusUseCase;
import com.eprocure.iam.application.usecase.ConfirmTwoFactorUseCase;
import com.eprocure.iam.application.usecase.CreateUserUseCase;
import com.eprocure.iam.application.usecase.EnableTwoFactorUseCase;
import com.eprocure.iam.application.usecase.GetCurrentUserUseCase;
import com.eprocure.iam.application.usecase.GetUserByIdUseCase;
import com.eprocure.iam.application.usecase.ListUsersUseCase;
import com.eprocure.iam.application.usecase.UpdateUserUseCase;
import com.eprocure.iam.common.api.ApiResponse;
import com.eprocure.iam.common.api.RequestIdUtil;
import com.eprocure.iam.common.security.UserPrincipal;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final ChangeUserStatusUseCase changeUserStatusUseCase;
    private final AssignUserRolesUseCase assignUserRolesUseCase;
    private final EnableTwoFactorUseCase enableTwoFactorUseCase;
    private final ConfirmTwoFactorUseCase confirmTwoFactorUseCase;

    public UserController(
            GetCurrentUserUseCase getCurrentUserUseCase,
            ListUsersUseCase listUsersUseCase,
            CreateUserUseCase createUserUseCase,
            GetUserByIdUseCase getUserByIdUseCase,
            UpdateUserUseCase updateUserUseCase,
            ChangeUserStatusUseCase changeUserStatusUseCase,
            AssignUserRolesUseCase assignUserRolesUseCase,
            EnableTwoFactorUseCase enableTwoFactorUseCase,
            ConfirmTwoFactorUseCase confirmTwoFactorUseCase) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.createUserUseCase = createUserUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.changeUserStatusUseCase = changeUserStatusUseCase;
        this.assignUserRolesUseCase = assignUserRolesUseCase;
        this.enableTwoFactorUseCase = enableTwoFactorUseCase;
        this.confirmTwoFactorUseCase = confirmTwoFactorUseCase;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('IAM_PROFILE_READ')")
    public ResponseEntity<ApiResponse<CurrentUserView>> getMe(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/users/me | userId={}", LogMaskingUtil.maskId(principal.getId()));
        return ResponseEntity.ok(ApiResponse.success(
                getCurrentUserUseCase.execute(principal.getId()),
                RequestIdUtil.resolve(request)));
    }

    @PutMapping("/me/two-factor/enable")
    @PreAuthorize("hasAuthority('IAM_PROFILE_READ')")
    public ResponseEntity<ApiResponse<TwoFactorSetupView>> enableTwoFactor(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PUT /api/v1/users/me/two-factor/enable | userId={}",
                LogMaskingUtil.maskId(principal.getId()));
        TwoFactorSetupView view = enableTwoFactorUseCase.execute(
                new EnableTwoFactorCommand(principal.getId()),
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(view, RequestIdUtil.resolve(request)));
    }

    @PutMapping("/me/two-factor/confirm")
    @PreAuthorize("hasAuthority('IAM_PROFILE_READ')")
    public ResponseEntity<ApiResponse<TwoFactorConfirmView>> confirmTwoFactor(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TwoFactorConfirmRequest body,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PUT /api/v1/users/me/two-factor/confirm | userId={}",
                LogMaskingUtil.maskId(principal.getId()));
        TwoFactorConfirmView view = confirmTwoFactorUseCase.execute(
                new ConfirmTwoFactorCommand(principal.getId(), body.code()),
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(view, RequestIdUtil.resolve(request)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN_USER_VIEW')")
    public ResponseEntity<ApiResponse<List<UserSummaryView>>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(name = "department_id", required = false) UUID departmentId,
            @RequestParam(required = false) String role,
            @RequestParam(name = "q", required = false) String query,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/users | userId={} | page={} | size={}",
                LogMaskingUtil.maskId(principal.getId()),
                page,
                size);
        PageResult<UserSummaryView> result = listUsersUseCase.execute(
                new ListUsersQuery(page, size, sort, status, departmentId, role, query));
        return ResponseEntity.ok(ApiResponse.success(result.items(), result.meta(), RequestIdUtil.resolve(request)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN_USER_MANAGE')")
    public ResponseEntity<ApiResponse<UserDetailView>> createUser(
            @Valid @RequestBody CreateUserRequest body,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/users | userId={}", LogMaskingUtil.maskId(principal.getId()));
        UserDetailView view = createUserUseCase.execute(
                new CreateUserCommand(
                        principal.getId(),
                        body.employeeCode(),
                        body.username(),
                        body.email(),
                        body.fullName(),
                        body.phone(),
                        body.departmentId(),
                        body.orgNodeId(),
                        body.roles()),
                idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(view, RequestIdUtil.resolve(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN_USER_VIEW')")
    public ResponseEntity<ApiResponse<UserDetailView>> getUserById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/users/{} | userId={}",
                LogMaskingUtil.maskId(id),
                LogMaskingUtil.maskId(principal.getId()));
        return ResponseEntity.ok(ApiResponse.success(
                getUserByIdUseCase.execute(id),
                RequestIdUtil.resolve(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN_USER_MANAGE')")
    public ResponseEntity<ApiResponse<UserDetailView>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest body,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PUT /api/v1/users/{} | userId={}",
                LogMaskingUtil.maskId(id),
                LogMaskingUtil.maskId(principal.getId()));
        UserDetailView view = updateUserUseCase.execute(
                new UpdateUserCommand(principal.getId(), id, body.fullName(), body.phone(), body.departmentId(), body.orgNodeId()),
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(view, RequestIdUtil.resolve(request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN_USER_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> changeUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeUserStatusRequest body,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PATCH /api/v1/users/{}/status | userId={}",
                LogMaskingUtil.maskId(id),
                LogMaskingUtil.maskId(principal.getId()));
        changeUserStatusUseCase.execute(
                new ChangeUserStatusCommand(principal.getId(), id, body.status(), body.reason()),
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.successMessage("User status updated", RequestIdUtil.resolve(request)));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ADMIN_ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> assignRoles(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRolesRequest body,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/users/{}/roles | userId={}",
                LogMaskingUtil.maskId(id),
                LogMaskingUtil.maskId(principal.getId()));
        assignUserRolesUseCase.execute(
                new AssignUserRolesCommand(principal.getId(), id, body.roles()),
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.successMessage("User roles updated", RequestIdUtil.resolve(request)));
    }
}
