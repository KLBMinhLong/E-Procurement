package com.eprocure.finance.infrastructure.http;

import com.eprocure.finance.application.port.out.PurchaseRequestConversionCallbackPort;
import com.eprocure.finance.application.port.out.PurchaseRequestPoSourcePort;
import com.eprocure.finance.application.service.PurchaseRequestPoSource;
import com.eprocure.finance.common.api.ApiResponse;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PurchaseRequestServiceAdapter implements PurchaseRequestPoSourcePort, PurchaseRequestConversionCallbackPort {
    private static final Logger log = LogManager.getLogger(PurchaseRequestServiceAdapter.class);
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final String internalApiKey;

    public PurchaseRequestServiceAdapter(
            RestClient.Builder builder,
            @Value("${eprocure.finance.integration.purchase-request-base-url:http://purchase-request-service:8082}") String baseUrl,
            @Value("${eprocure.internal.api-key:}") String internalApiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    @Override
    public PurchaseRequestPoSource fetch(UUID purchaseRequestId) {
        try {
            ApiResponse<PurchaseRequestPoSource> response = restClient.get()
                    .uri("/internal/purchase-requests/{id}/po-source", purchaseRequestId)
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null || !response.success() || response.data() == null) {
                throw new BusinessException(ErrorCode.FIN_017);
            }
            return response.data();
        } catch (RestClientResponseException exception) {
            log.warn("[ACTION] PR PO source rejected | prId={} | status={}",
                    LogMaskingUtil.maskId(purchaseRequestId),
                    exception.getStatusCode().value());
            throw new BusinessException(ErrorCode.FIN_017);
        }
    }

    @Override
    public void markConverted(UUID purchaseRequestId, UUID purchaseOrderId, String purchaseOrderNumber, UUID idempotencyKey) {
        try {
            restClient.patch()
                    .uri("/internal/purchase-requests/{id}/converted-to-po", purchaseRequestId)
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .header("Idempotency-Key", idempotencyKey.toString())
                    .body(new MarkConvertedRequest(purchaseOrderId, purchaseOrderNumber))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            log.warn("[ACTION] PR converted callback rejected | prId={} | poId={} | status={}",
                    LogMaskingUtil.maskId(purchaseRequestId),
                    LogMaskingUtil.maskId(purchaseOrderId),
                    exception.getStatusCode().value());
            throw exception;
        }
    }

    private record MarkConvertedRequest(UUID poId, String poNumber) {
    }
}
