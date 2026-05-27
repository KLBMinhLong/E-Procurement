package com.eprocure.pr.presentation.internal;

import com.eprocure.pr.application.port.in.MarkPurchaseRequestPendingApprovalCommand;
import com.eprocure.pr.application.service.InternalApiKeyGuard;
import com.eprocure.pr.application.service.MarkedPendingApprovalView;
import com.eprocure.pr.application.usecase.MarkPurchaseRequestPendingApprovalUseCase;
import com.eprocure.pr.common.api.ApiResponse;
import com.eprocure.pr.common.api.RequestIdUtil;
import com.eprocure.pr.common.util.LogMaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
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

    public InternalPurchaseRequestController(
            InternalApiKeyGuard internalApiKeyGuard,
            MarkPurchaseRequestPendingApprovalUseCase markPendingApprovalUseCase) {
        this.internalApiKeyGuard = internalApiKeyGuard;
        this.markPendingApprovalUseCase = markPendingApprovalUseCase;
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
}
