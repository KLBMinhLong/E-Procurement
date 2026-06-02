package com.eprocure.pr.presentation.internal;

import com.eprocure.pr.application.port.in.ApplyPurchaseRequestApprovalResultCommand;
import com.eprocure.pr.application.port.in.MarkPurchaseRequestPendingApprovalCommand;
import com.eprocure.pr.application.service.AppliedApprovalResultView;
import com.eprocure.pr.application.service.InternalApiKeyGuard;
import com.eprocure.pr.application.service.MarkedPendingApprovalView;
import com.eprocure.pr.application.service.RfqSourceView;
import com.eprocure.pr.application.usecase.ApplyPurchaseRequestApprovalResultUseCase;
import com.eprocure.pr.application.usecase.GetPurchaseRequestUseCase;
import com.eprocure.pr.application.usecase.MarkPurchaseRequestPendingApprovalUseCase;
import com.eprocure.pr.common.api.ApiResponse;
import com.eprocure.pr.common.api.RequestIdUtil;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.domain.model.PrStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/purchase-requests")
public class InternalPurchaseRequestController {
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final Logger log = LogManager.getLogger(InternalPurchaseRequestController.class);

    private final InternalApiKeyGuard internalApiKeyGuard;
    private final MarkPurchaseRequestPendingApprovalUseCase markPendingApprovalUseCase;
    private final ApplyPurchaseRequestApprovalResultUseCase applyApprovalResultUseCase;
    private final GetPurchaseRequestUseCase getPurchaseRequestUseCase;

    public InternalPurchaseRequestController(
            InternalApiKeyGuard internalApiKeyGuard,
            MarkPurchaseRequestPendingApprovalUseCase markPendingApprovalUseCase,
            ApplyPurchaseRequestApprovalResultUseCase applyApprovalResultUseCase,
            GetPurchaseRequestUseCase getPurchaseRequestUseCase) {
        this.internalApiKeyGuard = internalApiKeyGuard;
        this.markPendingApprovalUseCase = markPendingApprovalUseCase;
        this.applyApprovalResultUseCase = applyApprovalResultUseCase;
        this.getPurchaseRequestUseCase = getPurchaseRequestUseCase;
    }

    @GetMapping("/{id}/rfq-source")
    public ResponseEntity<ApiResponse<RfqSourceView>> getRfqSource(
            @PathVariable UUID id,
            @RequestHeader(value = INTERNAL_API_KEY_HEADER, required = false) String internalApiKey,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] GET /internal/purchase-requests/{}/rfq-source | userId=internal",
                LogMaskingUtil.maskId(id));
        return ResponseEntity.ok(ApiResponse.success(
                getPurchaseRequestUseCase.getRfqSource(id),
                RequestIdUtil.resolve(request)));
    }

    @PatchMapping("/{id}/pending-approval")
    public ResponseEntity<ApiResponse<MarkedPendingApprovalView>> markPendingApproval(
            @PathVariable UUID id,
            @Valid @RequestBody MarkPendingApprovalRequest body,
            @RequestHeader(value = INTERNAL_API_KEY_HEADER, required = false) String internalApiKey,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] PATCH /internal/purchase-requests/{}/pending-approval | userId=internal",
                LogMaskingUtil.maskId(id));
        MarkedPendingApprovalView view = markPendingApprovalUseCase.execute(
                new MarkPurchaseRequestPendingApprovalCommand(
                        id,
                        body.approvalProcessId(),
                        body.camundaProcessInstanceId()),
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(view, RequestIdUtil.resolve(request)));
    }

    @PatchMapping("/{id}/approved")
    public ResponseEntity<ApiResponse<AppliedApprovalResultView>> markApproved(
            @PathVariable UUID id,
            @Valid @RequestBody ApprovalResultRequest body,
            @RequestHeader(value = INTERNAL_API_KEY_HEADER, required = false) String internalApiKey,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            HttpServletRequest request) {
        return applyApprovalResult(id, body, PrStatus.APPROVED, internalApiKey, idempotencyKey, request);
    }

    @PatchMapping("/{id}/rejected")
    public ResponseEntity<ApiResponse<AppliedApprovalResultView>> markRejected(
            @PathVariable UUID id,
            @Valid @RequestBody ApprovalResultRequest body,
            @RequestHeader(value = INTERNAL_API_KEY_HEADER, required = false) String internalApiKey,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            HttpServletRequest request) {
        return applyApprovalResult(id, body, PrStatus.REJECTED, internalApiKey, idempotencyKey, request);
    }

    @PatchMapping("/{id}/changes-requested")
    public ResponseEntity<ApiResponse<AppliedApprovalResultView>> markChangesRequested(
            @PathVariable UUID id,
            @Valid @RequestBody ApprovalResultRequest body,
            @RequestHeader(value = INTERNAL_API_KEY_HEADER, required = false) String internalApiKey,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            HttpServletRequest request) {
        return applyApprovalResult(id, body, PrStatus.CHANGES_REQUESTED, internalApiKey, idempotencyKey, request);
    }

    private ResponseEntity<ApiResponse<AppliedApprovalResultView>> applyApprovalResult(
            UUID id,
            ApprovalResultRequest body,
            PrStatus targetStatus,
            String internalApiKey,
            String idempotencyKey,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] PATCH /internal/purchase-requests/{}/{} | userId=internal",
                LogMaskingUtil.maskId(id),
                targetStatus);
        AppliedApprovalResultView view = applyApprovalResultUseCase.execute(
                new ApplyPurchaseRequestApprovalResultCommand(
                        id,
                        body.approvalProcessId(),
                        targetStatus,
                        body.comment()),
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(view, RequestIdUtil.resolve(request)));
    }
}
