package com.eprocure.approval.presentation.controller;

import com.eprocure.approval.application.port.in.ApprovalTaskActionCommand;
import com.eprocure.approval.application.service.ApprovalTaskActionResult;
import com.eprocure.approval.application.service.ApprovalTaskActionView;
import com.eprocure.approval.application.usecase.ApprovalTaskActionUseCase;
import com.eprocure.approval.common.api.ApiResponse;
import com.eprocure.approval.common.api.RequestIdUtil;
import com.eprocure.approval.common.security.UserPrincipal;
import com.eprocure.approval.common.util.LogMaskingUtil;
import com.eprocure.approval.domain.model.ApprovalAction;
import com.eprocure.approval.presentation.request.ApprovalCommentRequest;
import com.eprocure.approval.presentation.request.ForwardTaskRequest;
import com.eprocure.approval.presentation.request.RequestChangesRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/approvals/tasks")
public class ApprovalTaskController {
    private static final Logger log = LogManager.getLogger(ApprovalTaskController.class);

    private final ApprovalTaskActionUseCase approvalTaskActionUseCase;
    private final com.eprocure.approval.application.usecase.GetTaskDetailUseCase getTaskDetailUseCase;

    public ApprovalTaskController(
            ApprovalTaskActionUseCase approvalTaskActionUseCase,
            com.eprocure.approval.application.usecase.GetTaskDetailUseCase getTaskDetailUseCase) {
        this.approvalTaskActionUseCase = approvalTaskActionUseCase;
        this.getTaskDetailUseCase = getTaskDetailUseCase;
    }

    @org.springframework.web.bind.annotation.GetMapping("/{taskId}")
    @PreAuthorize("hasAnyAuthority('PR_APPROVE_L1','PR_APPROVE_L2','PR_APPROVE_L3','PR_APPROVE_FINANCE','PR_APPROVE_EMERGENCY')")
    public ResponseEntity<ApiResponse<com.eprocure.approval.application.service.ApprovalTaskDetail>> getTaskDetail(
            @PathVariable String taskId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/approvals/tasks/{} | userId={}",
                taskId,
                LogMaskingUtil.maskId(principal.getId()));
        com.eprocure.approval.application.service.ApprovalTaskDetail result = getTaskDetailUseCase.execute(taskId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(result, RequestIdUtil.resolve(request)));
    }

    @PatchMapping("/{taskId}/approve")
    @PreAuthorize("hasAnyAuthority('PR_APPROVE_L1','PR_APPROVE_L2','PR_APPROVE_L3','PR_APPROVE_FINANCE','PR_APPROVE_EMERGENCY')")
    public ResponseEntity<ApiResponse<ApprovalTaskActionView>> approve(
            @PathVariable String taskId,
            @Valid @RequestBody(required = false) ApprovalCommentRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        return execute(taskId, ApprovalAction.APPROVE, body == null ? null : body.comment(), null, null,
                idempotencyKey, principal, request);
    }

    @PatchMapping("/{taskId}/reject")
    @PreAuthorize("hasAnyAuthority('PR_APPROVE_L1','PR_APPROVE_L2','PR_APPROVE_L3','PR_APPROVE_FINANCE','PR_APPROVE_EMERGENCY')")
    public ResponseEntity<ApiResponse<ApprovalTaskActionView>> reject(
            @PathVariable String taskId,
            @Valid @RequestBody ApprovalCommentRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        return execute(taskId, ApprovalAction.REJECT, body.comment(), null, null, idempotencyKey, principal, request);
    }

    @PatchMapping("/{taskId}/request-changes")
    @PreAuthorize("hasAuthority('PR_REQUEST_CHANGES')")
    public ResponseEntity<ApiResponse<ApprovalTaskActionView>> requestChanges(
            @PathVariable String taskId,
            @Valid @RequestBody RequestChangesRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        return execute(taskId, ApprovalAction.REQUEST_CHANGES, body.comment(), body.requestedFields(), null,
                idempotencyKey, principal, request);
    }

    @PatchMapping("/{taskId}/forward")
    @PreAuthorize("hasAuthority('PR_FORWARD')")
    public ResponseEntity<ApiResponse<ApprovalTaskActionView>> forward(
            @PathVariable String taskId,
            @Valid @RequestBody ForwardTaskRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        return execute(taskId, ApprovalAction.FORWARD, body.reason(), null, body.forwardToUserId(),
                idempotencyKey, principal, request);
    }

    private ResponseEntity<ApiResponse<ApprovalTaskActionView>> execute(
            String taskId,
            ApprovalAction action,
            String comment,
            java.util.List<String> requestedFields,
            java.util.UUID forwardToUserId,
            String idempotencyKey,
            UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PATCH /api/v1/approvals/tasks/{}/{} | userId={}",
                taskId,
                action,
                LogMaskingUtil.maskId(principal.getId()));
        ApprovalTaskActionResult result = approvalTaskActionUseCase.execute(
                new ApprovalTaskActionCommand(
                        taskId,
                        principal.getId(),
                        action,
                        comment,
                        requestedFields,
                        forwardToUserId),
                idempotencyKey);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(result.view(), RequestIdUtil.resolve(request)));
    }
}
