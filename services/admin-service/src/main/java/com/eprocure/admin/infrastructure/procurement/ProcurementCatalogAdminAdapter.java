package com.eprocure.admin.infrastructure.procurement;

import com.eprocure.admin.application.port.in.DeactivateCatalogCategoryCommand;
import com.eprocure.admin.application.port.in.ManageCatalogCategoryCommand;
import com.eprocure.admin.application.port.out.ProcurementCatalogAdminPort;
import com.eprocure.admin.application.service.CatalogCategoryAdminView;
import com.eprocure.admin.common.api.ApiResponse;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import com.eprocure.admin.common.util.LogMaskingUtil;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ProcurementCatalogAdminAdapter implements ProcurementCatalogAdminPort {
    private static final Logger log = LogManager.getLogger(ProcurementCatalogAdminAdapter.class);
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final String internalApiKey;

    public ProcurementCatalogAdminAdapter(
            RestClient.Builder builder,
            @Value("${eprocure.admin.integration.pr.base-url:http://localhost:8082}") String baseUrl,
            @Value("${eprocure.admin.integration.pr.internal-api-key:change-me-internal-api-key}") String internalApiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    @Override
    public List<CatalogCategoryAdminView> listCategories(boolean includeInactive) {
        try {
            ApiResponse<List<CatalogCategoryAdminView>> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/internal/catalog/categories")
                            .queryParam("include_inactive", includeInactive)
                            .build())
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null || !response.success() || response.data() == null) {
                throw new BusinessException(ErrorCode.SYS_002);
            }
            return response.data();
        } catch (RestClientResponseException exception) {
            throw mapException("PR catalog list rejected", exception, null, null);
        }
    }

    @Override
    public CatalogCategoryAdminView createCategory(ManageCatalogCategoryCommand command, UUID idempotencyKey) {
        return mutate(
                "create",
                restClient.post()
                        .uri("/internal/catalog/categories")
                        .body(toRequest(command)),
                command.code(),
                command.actorId(),
                idempotencyKey);
    }

    @Override
    public CatalogCategoryAdminView updateCategory(ManageCatalogCategoryCommand command, UUID idempotencyKey) {
        return mutate(
                "update",
                restClient.put()
                        .uri("/internal/catalog/categories/{code}", command.code())
                        .body(toRequest(command)),
                command.code(),
                command.actorId(),
                idempotencyKey);
    }

    @Override
    public CatalogCategoryAdminView deactivateCategory(DeactivateCatalogCategoryCommand command, UUID idempotencyKey) {
        return mutate(
                "deactivate",
                restClient.patch()
                        .uri("/internal/catalog/categories/{code}/deactivate", command.code())
                        .body(new DeactivateRequest(command.actorId())),
                command.code(),
                command.actorId(),
                idempotencyKey);
    }

    private CatalogCategoryAdminView mutate(
            String operation,
            RestClient.RequestHeadersSpec<?> requestSpec,
            String code,
            UUID actorId,
            UUID idempotencyKey) {
        try {
            ApiResponse<CatalogCategoryAdminView> response = requestSpec
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .header("Idempotency-Key", idempotencyKey.toString())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null || !response.success() || response.data() == null) {
                throw new BusinessException(ErrorCode.SYS_002);
            }
            return response.data();
        } catch (RestClientResponseException exception) {
            throw mapException("PR catalog " + operation + " rejected", exception, code, actorId);
        }
    }

    private BusinessException mapException(
            String action,
            RestClientResponseException exception,
            String code,
            UUID actorId) {
        log.warn("[ACTION] {} | status={} | code={} | actorId={}",
                action,
                exception.getStatusCode().value(),
                code,
                LogMaskingUtil.maskId(actorId));
        if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
            return new BusinessException(ErrorCode.CATALOG_CATEGORY_NOT_FOUND);
        }
        if (exception.getStatusCode() == HttpStatus.CONFLICT) {
            return new BusinessException(ErrorCode.CATALOG_CATEGORY_CONFLICT);
        }
        if (exception.getStatusCode() == HttpStatus.BAD_REQUEST) {
            return new BusinessException(ErrorCode.VAL_001);
        }
        return new BusinessException(ErrorCode.SYS_002);
    }

    private CatalogCategoryRequest toRequest(ManageCatalogCategoryCommand command) {
        return new CatalogCategoryRequest(
                command.actorId(),
                command.code(),
                command.name(),
                command.parentCode(),
                command.requiresSpecialApproval(),
                command.specialApproverRole(),
                command.requiresRfqAbove(),
                command.capex());
    }

    private record CatalogCategoryRequest(
            UUID actorId,
            String code,
            String name,
            String parentCode,
            boolean requiresSpecialApproval,
            String specialApproverRole,
            BigDecimal requiresRfqAbove,
            boolean isCapex) {
    }

    private record DeactivateRequest(UUID actorId) {
    }
}
