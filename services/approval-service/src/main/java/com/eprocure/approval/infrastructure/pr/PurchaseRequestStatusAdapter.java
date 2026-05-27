package com.eprocure.approval.infrastructure.pr;

import com.eprocure.approval.application.port.out.PurchaseRequestStatusPort;
import com.eprocure.approval.common.exception.BusinessException;
import com.eprocure.approval.common.exception.ErrorCode;
import com.eprocure.approval.common.util.LogMaskingUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PurchaseRequestStatusAdapter implements PurchaseRequestStatusPort {
    private static final Logger log = LogManager.getLogger(PurchaseRequestStatusAdapter.class);
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final RestClient purchaseRequestRestClient;
    private final String internalApiKey;

    public PurchaseRequestStatusAdapter(
            @Qualifier("purchaseRequestRestClient") RestClient purchaseRequestRestClient,
            @Value("${eprocure.integration.pr.internal-api-key:}") String internalApiKey) {
        this.purchaseRequestRestClient = purchaseRequestRestClient;
        this.internalApiKey = internalApiKey == null ? "" : internalApiKey;
    }

    @Override
    public void markPendingApproval(MarkPendingApprovalCommand command) {
        if (internalApiKey.isBlank()) {
            log.error("[ACTION] Step MarkPrPendingApproval | prId={} | result=missing_api_key",
                    LogMaskingUtil.maskId(command.purchaseRequestId()));
            throw new BusinessException(ErrorCode.SYS_001);
        }
        try {
            purchaseRequestRestClient.patch()
                    .uri("/internal/purchase-requests/{id}/pending-approval", command.purchaseRequestId())
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .header(IDEMPOTENCY_KEY_HEADER, command.approvalProcessId().toString())
                    .body(new MarkPendingApprovalRequest(
                            command.approvalProcessId(),
                            command.camundaProcessInstanceId()))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[ACTION] Step MarkPrPendingApproval | prId={} | approvalProcessId={} | result=success",
                    LogMaskingUtil.maskId(command.purchaseRequestId()),
                    LogMaskingUtil.maskId(command.approvalProcessId()));
        } catch (RestClientResponseException exception) {
            log.warn("[ACTION] Step MarkPrPendingApproval | prId={} | status={}",
                    LogMaskingUtil.maskId(command.purchaseRequestId()),
                    exception.getStatusCode().value());
            throw new BusinessException(ErrorCode.SYS_001);
        } catch (RestClientException exception) {
            log.error("[EXCEPTION][SYS_001] PR pending approval callback failed | prId={} | error={}",
                    LogMaskingUtil.maskId(command.purchaseRequestId()),
                    exception.getMessage());
            throw new BusinessException(ErrorCode.SYS_001);
        }
    }

    private record MarkPendingApprovalRequest(
            @NotNull UUID approvalProcessId,
            @NotBlank String camundaProcessInstanceId) {
    }
}
