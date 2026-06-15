package com.eprocure.admin.infrastructure.iam;

import com.eprocure.admin.application.port.out.IamSecurityConfirmationPort;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import com.eprocure.admin.common.util.LogMaskingUtil;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class IamSecurityConfirmationAdapter implements IamSecurityConfirmationPort {
    private static final Logger log = LogManager.getLogger(IamSecurityConfirmationAdapter.class);
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final String internalApiKey;

    public IamSecurityConfirmationAdapter(
            RestClient.Builder builder,
            @Value("${eprocure.admin.integration.iam.base-url:http://localhost:8081}") String baseUrl,
            @Value("${eprocure.admin.integration.iam.internal-api-key:change-me-internal-api-key}") String internalApiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    @Override
    public void verifyTotp(UUID userId, String confirmationCode) {
        try {
            restClient.post()
                    .uri("/internal/security/totp/verify")
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .body(new TotpVerificationRequest(userId, confirmationCode))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw mapException(exception, userId);
        }
    }

    private BusinessException mapException(RestClientResponseException exception, UUID userId) {
        log.warn("[ACTION] IAM TOTP confirmation rejected | status={} | userId={}",
                exception.getStatusCode().value(),
                LogMaskingUtil.maskId(userId));
        if (exception.getStatusCode() == HttpStatus.BAD_REQUEST
                || exception.getStatusCode() == HttpStatus.UNAUTHORIZED
                || exception.getStatusCode() == HttpStatus.NOT_FOUND) {
            return new BusinessException(ErrorCode.CONFIG_CONFIRMATION_FAILED);
        }
        if (exception.getStatusCode() == HttpStatus.FORBIDDEN) {
            return new BusinessException(ErrorCode.IAM_004);
        }
        return new BusinessException(ErrorCode.SYS_002);
    }

    private record TotpVerificationRequest(UUID userId, String code) {
    }
}
