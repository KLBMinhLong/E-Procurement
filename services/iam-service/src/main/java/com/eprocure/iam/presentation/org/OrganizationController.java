package com.eprocure.iam.presentation.org;

import com.eprocure.iam.application.port.in.ListDepartmentMembersQuery;
import com.eprocure.iam.application.port.in.ResolveApproversQuery;
import com.eprocure.iam.application.service.DepartmentDetailView;
import com.eprocure.iam.application.service.PageResult;
import com.eprocure.iam.application.service.UserSummaryView;
import com.eprocure.iam.application.usecase.GetDepartmentMembersUseCase;
import com.eprocure.iam.application.usecase.GetDepartmentTreeUseCase;
import com.eprocure.iam.application.usecase.ResolveApproversUseCase;
import com.eprocure.iam.common.api.ApiResponse;
import com.eprocure.iam.common.api.RequestIdUtil;
import com.eprocure.iam.common.security.UserPrincipal;
import com.eprocure.iam.common.util.LogMaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/org")
public class OrganizationController {
    private static final Logger log = LogManager.getLogger(OrganizationController.class);
    private final GetDepartmentTreeUseCase getDepartmentTreeUseCase;
    private final GetDepartmentMembersUseCase getDepartmentMembersUseCase;
    private final ResolveApproversUseCase resolveApproversUseCase;

    public OrganizationController(
            GetDepartmentTreeUseCase getDepartmentTreeUseCase,
            GetDepartmentMembersUseCase getDepartmentMembersUseCase,
            ResolveApproversUseCase resolveApproversUseCase) {
        this.getDepartmentTreeUseCase = getDepartmentTreeUseCase;
        this.getDepartmentMembersUseCase = getDepartmentMembersUseCase;
        this.resolveApproversUseCase = resolveApproversUseCase;
    }

    @GetMapping("/departments")
    @PreAuthorize("hasAuthority('ORG_VIEW')")
    public ResponseEntity<ApiResponse<List<DepartmentDetailView>>> getDepartmentTree(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/org/departments | userId={}", LogMaskingUtil.maskId(principal.getId()));
        return ResponseEntity.ok(ApiResponse.success(getDepartmentTreeUseCase.execute(), RequestIdUtil.resolve(request)));
    }

    @GetMapping("/departments/{id}/members")
    @PreAuthorize("hasAuthority('ORG_VIEW')")
    public ResponseEntity<ApiResponse<List<UserSummaryView>>> getDepartmentMembers(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/org/departments/{}/members | userId={} | page={} | size={}",
                LogMaskingUtil.maskId(id),
                LogMaskingUtil.maskId(principal.getId()),
                page,
                size);
        PageResult<UserSummaryView> result = getDepartmentMembersUseCase.execute(new ListDepartmentMembersQuery(id, page, size));
        return ResponseEntity.ok(ApiResponse.success(result.items(), result.meta(), RequestIdUtil.resolve(request)));
    }

    @GetMapping("/approvers")
    @PreAuthorize("hasAuthority('ORG_APPROVER_RESOLVE')")
    public ResponseEntity<ApiResponse<List<UserSummaryView>>> getApprovers(
            @RequestParam(name = "role") String roleCode,
            @RequestParam(name = "department_id", required = false) UUID departmentId,
            @RequestParam(name = "requester_id", required = false) UUID requesterId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        UUID excludedUserId = requesterId == null ? principal.getId() : requesterId;
        log.info("[CONTROLLER] GET /api/v1/org/approvers | userId={} | roleCode={} | departmentId={} | requesterId={}",
                LogMaskingUtil.maskId(principal.getId()),
                roleCode,
                LogMaskingUtil.maskId(departmentId),
                LogMaskingUtil.maskId(excludedUserId));
        return ResponseEntity.ok(ApiResponse.success(
                resolveApproversUseCase.execute(new ResolveApproversQuery(roleCode, departmentId, excludedUserId)),
                RequestIdUtil.resolve(request)));
    }
}
