package com.eprocure.finance.infrastructure.http;

import com.eprocure.finance.application.port.out.VendorPoSourcePort;
import com.eprocure.finance.application.service.VendorPoSource;
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
public class VendorServiceAdapter implements VendorPoSourcePort {
    private static final Logger log = LogManager.getLogger(VendorServiceAdapter.class);
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final String internalApiKey;

    public VendorServiceAdapter(
            RestClient.Builder builder,
            @Value("${eprocure.finance.integration.vendor-base-url:http://vendor-service:8086}") String baseUrl,
            @Value("${eprocure.internal.api-key:}") String internalApiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    @Override
    public VendorPoSource fetch(UUID vendorId) {
        try {
            ApiResponse<VendorPoSource> response = restClient.get()
                    .uri("/internal/vendors/{id}/po-source", vendorId)
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null || !response.success() || response.data() == null) {
                throw new BusinessException(ErrorCode.FIN_019);
            }
            return response.data();
        } catch (RestClientResponseException exception) {
            log.warn("[ACTION] Vendor PO source rejected | vendorId={} | status={}",
                    LogMaskingUtil.maskId(vendorId),
                    exception.getStatusCode().value());
            throw new BusinessException(ErrorCode.FIN_019);
        }
    }
}
