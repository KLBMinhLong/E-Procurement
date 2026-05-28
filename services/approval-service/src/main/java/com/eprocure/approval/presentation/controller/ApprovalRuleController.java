package com.eprocure.approval.presentation.controller;

import com.eprocure.approval.application.port.in.ApprovalRuleConditionCommand;
import com.eprocure.approval.application.port.in.ApprovalRuleStepCommand;
import com.eprocure.approval.application.port.in.CreateApprovalRuleCommand;
import com.eprocure.approval.application.port.in.DeactivateApprovalRuleCommand;
import com.eprocure.approval.application.port.in.UpdateApprovalRuleCommand;
import com.eprocure.approval.application.service.ApprovalRuleAdminView;
import com.eprocure.approval.application.service.ApprovalRuleMutationResult;
import com.eprocure.approval.application.usecase.CreateApprovalRuleUseCase;
import com.eprocure.approval.application.usecase.DeactivateApprovalRuleUseCase;
import com.eprocure.approval.application.usecase.ListApprovalRulesUseCase;
import com.eprocure.approval.application.usecase.UpdateApprovalRuleUseCase;
import com.eprocure.approval.common.api.ApiResponse;
import com.eprocure.approval.common.api.RequestIdUtil;
import com.eprocure.approval.common.security.UserPrincipal;
import com.eprocure.approval.common.util.LogMaskingUtil;
import com.eprocure.approval.presentation.request.ApprovalRuleConditionRequest;
import com.eprocure.approval.presentation.request.ApprovalRuleRequest;
import com.eprocure.approval.presentation.request.ApprovalRuleStepRequest;
import com.eprocure.approval.presentation.request.DeactivateApprovalRuleRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/approvals/rules")
public class ApprovalRuleController {
    private static final Logger log = LogManager.getLogger(ApprovalRuleController.class);

    private final ListApprovalRulesUseCase listApprovalRulesUseCase;
    private final CreateApprovalRuleUseCase createApprovalRuleUseCase;
    private final UpdateApprovalRuleUseCase updateApprovalRuleUseCase;
    private final DeactivateApprovalRuleUseCase deactivateApprovalRuleUseCase;

    public ApprovalRuleController(
            ListApprovalRulesUseCase listApprovalRulesUseCase,
            CreateApprovalRuleUseCase createApprovalRuleUseCase,
            UpdateApprovalRuleUseCase updateApprovalRuleUseCase,
            DeactivateApprovalRuleUseCase deactivateApprovalRuleUseCase) {
        this.listApprovalRulesUseCase = listApprovalRulesUseCase;
        this.createApprovalRuleUseCase = createApprovalRuleUseCase;
        this.updateApprovalRuleUseCase = updateApprovalRuleUseCase;
        this.deactivateApprovalRuleUseCase = deactivateApprovalRuleUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN_APPROVAL_RULE')")
    public ResponseEntity<ApiResponse<List<ApprovalRuleAdminView>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/approvals/rules | userId={}", LogMaskingUtil.maskId(principal.getId()));
        return ResponseEntity.ok(ApiResponse.success(listApprovalRulesUseCase.execute(), RequestIdUtil.resolve(request)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN_APPROVAL_RULE')")
    public ResponseEntity<ApiResponse<ApprovalRuleAdminView>> create(
            @Valid @RequestBody ApprovalRuleRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/approvals/rules | userId={} | ruleName={}",
                LogMaskingUtil.maskId(principal.getId()),
                body.ruleName());
        ApprovalRuleMutationResult result = createApprovalRuleUseCase.execute(
                new CreateApprovalRuleCommand(
                        principal.getId(),
                        body.ruleName(),
                        body.priority(),
                        body.ruleType(),
                        toConditionCommand(body.conditions()),
                        toStepCommands(body.steps()),
                        body.description()),
                idempotencyKey);
        return response(result, request, result.replayed() ? HttpStatus.OK : HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN_APPROVAL_RULE')")
    public ResponseEntity<ApiResponse<ApprovalRuleAdminView>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ApprovalRuleRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PUT /api/v1/approvals/rules/{} | userId={}",
                id,
                LogMaskingUtil.maskId(principal.getId()));
        ApprovalRuleMutationResult result = updateApprovalRuleUseCase.execute(
                new UpdateApprovalRuleCommand(
                        id,
                        principal.getId(),
                        body.ruleName(),
                        body.priority(),
                        body.active(),
                        body.ruleType(),
                        toConditionCommand(body.conditions()),
                        toStepCommands(body.steps()),
                        body.description()),
                idempotencyKey);
        return response(result, request, HttpStatus.OK);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('ADMIN_APPROVAL_RULE')")
    public ResponseEntity<ApiResponse<ApprovalRuleAdminView>> deactivate(
            @PathVariable UUID id,
            @Valid @RequestBody DeactivateApprovalRuleRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PATCH /api/v1/approvals/rules/{}/deactivate | userId={}",
                id,
                LogMaskingUtil.maskId(principal.getId()));
        ApprovalRuleMutationResult result = deactivateApprovalRuleUseCase.execute(
                new DeactivateApprovalRuleCommand(id, principal.getId(), body.reason()),
                idempotencyKey);
        return response(result, request, HttpStatus.OK);
    }

    private ResponseEntity<ApiResponse<ApprovalRuleAdminView>> response(
            ApprovalRuleMutationResult result,
            HttpServletRequest request,
            HttpStatus status) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(result.view(), RequestIdUtil.resolve(request)));
    }

    private ApprovalRuleConditionCommand toConditionCommand(ApprovalRuleConditionRequest request) {
        if (request == null) {
            return null;
        }
        return new ApprovalRuleConditionCommand(
                request.minValue(),
                request.maxValue(),
                request.categories(),
                request.departmentIds(),
                request.priorities());
    }

    private List<ApprovalRuleStepCommand> toStepCommands(List<ApprovalRuleStepRequest> steps) {
        return steps.stream()
                .map(step -> new ApprovalRuleStepCommand(
                        step.stepIndex(),
                        step.approverRole(),
                        step.stepType(),
                        step.slaHours(),
                        step.required()))
                .toList();
    }
}
