package com.eprocure.approval.presentation.controller;

import com.eprocure.approval.application.port.in.GetApprovalInboxQuery;
import com.eprocure.approval.application.service.ApprovalInboxCount;
import com.eprocure.approval.application.service.ApprovalTaskSummary;
import com.eprocure.approval.application.service.PageResult;
import com.eprocure.approval.application.usecase.CountPendingTasksUseCase;
import com.eprocure.approval.application.usecase.GetApprovalInboxUseCase;
import com.eprocure.approval.common.api.ApiResponse;
import com.eprocure.approval.common.api.RequestIdUtil;
import com.eprocure.approval.common.security.UserPrincipal;
import com.eprocure.approval.common.util.LogMaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalInboxController {
    private static final Logger log = LogManager.getLogger(ApprovalInboxController.class);

    private final GetApprovalInboxUseCase getApprovalInboxUseCase;
    private final CountPendingTasksUseCase countPendingTasksUseCase;

    public ApprovalInboxController(
            GetApprovalInboxUseCase getApprovalInboxUseCase,
            CountPendingTasksUseCase countPendingTasksUseCase) {
        this.getApprovalInboxUseCase = getApprovalInboxUseCase;
        this.countPendingTasksUseCase = countPendingTasksUseCase;
    }

    @GetMapping("/inbox")
    @PreAuthorize("hasAnyAuthority('PR_APPROVE_L1','PR_APPROVE_L2','PR_APPROVE_L3','PR_APPROVE_FINANCE','PR_APPROVE_EMERGENCY')")
    public ResponseEntity<ApiResponse<List<ApprovalTaskSummary>>> getInbox(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String minAmount,
            @RequestParam(required = false) Boolean isOverdue,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        log.info("[CONTROLLER] GET /api/v1/approvals/inbox | userId={}", LogMaskingUtil.maskId(principal.getId()));

        GetApprovalInboxQuery query = new GetApprovalInboxQuery(
                principal.getId(),
                page,
                size,
                sort,
                priority,
                entityType,
                minAmount,
                isOverdue
        );

        PageResult<ApprovalTaskSummary> result = getApprovalInboxUseCase.execute(query);
        return ResponseEntity.ok(ApiResponse.success(result.items(), result.meta(), RequestIdUtil.resolve(request)));
    }

    @GetMapping("/inbox/count")
    @PreAuthorize("hasAnyAuthority('PR_APPROVE_L1','PR_APPROVE_L2','PR_APPROVE_L3','PR_APPROVE_FINANCE','PR_APPROVE_EMERGENCY')")
    public ResponseEntity<ApiResponse<ApprovalInboxCount>> getInboxCount(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        log.info("[CONTROLLER] GET /api/v1/approvals/inbox/count | userId={}", LogMaskingUtil.maskId(principal.getId()));

        ApprovalInboxCount result = countPendingTasksUseCase.execute(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(result, RequestIdUtil.resolve(request)));
    }
}
