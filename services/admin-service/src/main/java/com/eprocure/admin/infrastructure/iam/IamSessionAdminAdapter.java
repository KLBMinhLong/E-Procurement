package com.eprocure.admin.infrastructure.iam;

import com.eprocure.admin.application.port.out.IamSessionAdminPort;
import com.eprocure.admin.application.service.ActiveSessionView;
import com.eprocure.admin.application.service.PageMeta;
import com.eprocure.admin.application.service.PageResult;
import com.eprocure.admin.common.api.ApiResponse;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class IamSessionAdminAdapter implements IamSessionAdminPort {
    private static final Logger log = LogManager.getLogger(IamSessionAdminAdapter.class);
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String internalApiKey;

    public IamSessionAdminAdapter(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${eprocure.admin.integration.iam.base-url:http://localhost:8081}") String baseUrl,
            @Value("${eprocure.admin.integration.iam.internal-api-key:change-me-internal-api-key}") String internalApiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.internalApiKey = internalApiKey;
    }

    @Override
    public PageResult<ActiveSessionView> listActiveSessions(UUID userId, int page, int size) {
        try {
            ApiResponse<List<ActiveSessionView>> response = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/internal/sessions")
                                .queryParam("page", page)
                                .queryParam("size", size);
                        if (userId != null) {
                            builder.queryParam("user_id", userId);
                        }
                        return builder.build();
                    })
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null || !response.success() || response.data() == null) {
                throw new BusinessException(ErrorCode.SYS_002);
            }
            return new PageResult<>(response.data(), toPageMeta(response.meta()));
        } catch (RestClientResponseException exception) {
            log.warn("[ACTION] IAM active sessions request rejected | status={} | filterUserId={}",
                    exception.getStatusCode().value(),
                    LogMaskingUtil.maskId(userId));
            throw new BusinessException(ErrorCode.SYS_002);
        }
    }

    @Override
    public void invalidateSession(UUID sessionId, UUID actorId, String reason, UUID idempotencyKey) {
        try {
            restClient.patch()
                    .uri("/internal/sessions/{sessionId}/invalidate", sessionId)
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .header("Idempotency-Key", idempotencyKey.toString())
                    .body(new InvalidateSessionRequest(actorId, reason))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            log.warn("[ACTION] IAM invalidate session request rejected | status={} | sessionId={} | actorId={}",
                    exception.getStatusCode().value(),
                    LogMaskingUtil.maskId(sessionId),
                    LogMaskingUtil.maskId(actorId));
            throw new BusinessException(ErrorCode.SYS_002);
        }
    }

    private PageMeta toPageMeta(Object value) {
        if (value == null) {
            return PageMeta.of(0, 1, 50, "issuedAt,desc");
        }
        return objectMapper.convertValue(value, PageMeta.class);
    }

    private record InvalidateSessionRequest(UUID actorId, String reason) {
    }
}
