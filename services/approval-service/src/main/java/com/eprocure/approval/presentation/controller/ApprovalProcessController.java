package com.eprocure.approval.presentation.controller;

import com.eprocure.approval.application.service.ApprovalProcessDetail;
import com.eprocure.approval.application.usecase.GetApprovalProcessUseCase;
import com.eprocure.approval.common.api.ApiResponse;
import com.eprocure.approval.common.api.RequestIdUtil;
import com.eprocure.approval.common.security.UserPrincipal;
import com.eprocure.approval.common.util.LogMaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/approvals/processes")
public class ApprovalProcessController {
    private static final Logger log = LogManager.getLogger(ApprovalProcessController.class);

    private final GetApprovalProcessUseCase getApprovalProcessUseCase;

    public ApprovalProcessController(GetApprovalProcessUseCase getApprovalProcessUseCase) {
        this.getApprovalProcessUseCase = getApprovalProcessUseCase;
    }

    @GetMapping("/{entityType}/{entityId}")
    @PreAuthorize("hasAnyAuthority('PR_VIEW_OWN','PR_VIEW_DEPARTMENT','PR_VIEW_ALL','PR_APPROVE_L1','PR_APPROVE_L2','PR_APPROVE_L3','PR_APPROVE_FINANCE','PR_APPROVE_EMERGENCY')")
    public ResponseEntity<ApiResponse<ApprovalProcessDetail>> getProcess(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/approvals/processes/{}/{} | userId={}",
                entityType,
                entityId,
                LogMaskingUtil.maskId(principal.getId()));
        ApprovalProcessDetail result = getApprovalProcessUseCase.execute(
                entityType,
                entityId,
                principal.getId(),
                principal.getDepartmentId(),
                principal.getPermissions());
        return ResponseEntity.ok(ApiResponse.success(result, RequestIdUtil.resolve(request)));
    }
}
