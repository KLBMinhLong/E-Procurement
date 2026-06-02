package com.eprocure.vendor.infrastructure.purchase;

import com.eprocure.vendor.application.port.out.PurchaseRequestRfqSourcePort;
import com.eprocure.vendor.common.api.ApiResponse;
import com.eprocure.vendor.common.exception.BusinessException;
import com.eprocure.vendor.common.exception.ErrorCode;
import com.eprocure.vendor.common.util.LogMaskingUtil;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PrServiceRfqSourceAdapter implements PurchaseRequestRfqSourcePort {
    private static final Logger log = LogManager.getLogger(PrServiceRfqSourceAdapter.class);
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final ParameterizedTypeReference<ApiResponse<PrRfqSourceResponse>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient purchaseRequestRestClient;
    private final String internalApiKey;

    public PrServiceRfqSourceAdapter(
            @Qualifier("purchaseRequestRestClient") RestClient purchaseRequestRestClient,
            @Value("${eprocure.vendor.integration.pr.internal-api-key:}") String internalApiKey) {
        this.purchaseRequestRestClient = purchaseRequestRestClient;
        this.internalApiKey = internalApiKey == null ? "" : internalApiKey;
    }

    @Override
    public PurchaseRequestRfqSource getSource(UUID purchaseRequestId) {
        if (internalApiKey.isBlank()) {
            log.error("[ACTION] Step PurchaseRequestRfqSource | prId={} | result=missing_api_key",
                    LogMaskingUtil.maskId(purchaseRequestId));
            throw new BusinessException(ErrorCode.SYS_001);
        }
        try {
            ApiResponse<PrRfqSourceResponse> response = purchaseRequestRestClient.get()
                    .uri("/internal/purchase-requests/{id}/rfq-source", purchaseRequestId)
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .body(RESPONSE_TYPE);
            if (response == null || response.data() == null) {
                throw new BusinessException(ErrorCode.SYS_001);
            }
            PrRfqSourceResponse data = response.data();
            log.info("[ACTION] Step PurchaseRequestRfqSource | prId={} | status={}",
                    LogMaskingUtil.maskId(purchaseRequestId),
                    data.status());
            return data.toDomain();
        } catch (RestClientResponseException exception) {
            log.warn("[ACTION] Step PurchaseRequestRfqSource | prId={} | status={}",
                    LogMaskingUtil.maskId(purchaseRequestId),
                    exception.getStatusCode().value());
            int status = exception.getStatusCode().value();
            if (status == 404 || status == 409 || status == 422) {
                throw new BusinessException(ErrorCode.VND_009);
            }
            throw new BusinessException(ErrorCode.SYS_001);
        } catch (RestClientException exception) {
            log.error("[EXCEPTION][SYS_001] Purchase request RFQ source failed | prId={} | error={}",
                    LogMaskingUtil.maskId(purchaseRequestId),
                    exception.getMessage());
            throw new BusinessException(ErrorCode.SYS_001);
        }
    }

    public record PrRfqSourceLineItemResponse(
            UUID id,
            String itemName,
            String categoryCode,
            BigDecimal quantity,
            String unit,
            String specifications) {

        PurchaseRequestRfqLineItem toDomain() {
            return new PurchaseRequestRfqLineItem(id, itemName, categoryCode, quantity, unit, specifications);
        }
    }

    public record PrRfqSourceResponse(
            UUID id,
            String prNumber,
            String status,
            List<PrRfqSourceLineItemResponse> lineItems) {

        PurchaseRequestRfqSource toDomain() {
            return new PurchaseRequestRfqSource(
                    id,
                    prNumber,
                    status,
                    lineItems == null ? List.of() : lineItems.stream().map(PrRfqSourceLineItemResponse::toDomain).toList());
        }
    }
}
