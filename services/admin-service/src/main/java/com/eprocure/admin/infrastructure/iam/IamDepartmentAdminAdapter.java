package com.eprocure.admin.infrastructure.iam;

import com.eprocure.admin.application.port.in.DeactivateDepartmentCommand;
import com.eprocure.admin.application.port.in.ManageDepartmentCommand;
import com.eprocure.admin.application.port.out.IamDepartmentAdminPort;
import com.eprocure.admin.application.service.DepartmentAdminView;
import com.eprocure.admin.common.api.ApiResponse;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import com.eprocure.admin.common.util.LogMaskingUtil;
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
public class IamDepartmentAdminAdapter implements IamDepartmentAdminPort {
    private static final Logger log = LogManager.getLogger(IamDepartmentAdminAdapter.class);
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final String internalApiKey;

    public IamDepartmentAdminAdapter(
            RestClient.Builder builder,
            @Value("${eprocure.admin.integration.iam.base-url:http://localhost:8081}") String baseUrl,
            @Value("${eprocure.admin.integration.iam.internal-api-key:change-me-internal-api-key}") String internalApiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    @Override
    public DepartmentAdminView createDepartment(ManageDepartmentCommand command, UUID idempotencyKey) {
        return mutate(
                "create department",
                restClient.post()
                        .uri("/internal/org/departments")
                        .body(toRequest(command)),
                command.departmentId(),
                command.actorId(),
                idempotencyKey);
    }

    @Override
    public DepartmentAdminView updateDepartment(ManageDepartmentCommand command, UUID idempotencyKey) {
        return mutate(
                "update department",
                restClient.put()
                        .uri("/internal/org/departments/{id}", command.departmentId())
                        .body(toRequest(command)),
                command.departmentId(),
                command.actorId(),
                idempotencyKey);
    }

    @Override
    public DepartmentAdminView deactivateDepartment(DeactivateDepartmentCommand command, UUID idempotencyKey) {
        return mutate(
                "deactivate department",
                restClient.patch()
                        .uri("/internal/org/departments/{id}/deactivate", command.departmentId())
                        .body(new DeactivateRequest(command.actorId())),
                command.departmentId(),
                command.actorId(),
                idempotencyKey);
    }

    private DepartmentAdminView mutate(
            String operation,
            RestClient.RequestHeadersSpec<?> requestSpec,
            UUID departmentId,
            UUID actorId,
            UUID idempotencyKey) {
        try {
            ApiResponse<DepartmentAdminView> response = requestSpec
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
            throw mapException(operation, exception, departmentId, actorId);
        }
    }

    private BusinessException mapException(
            String operation,
            RestClientResponseException exception,
            UUID departmentId,
            UUID actorId) {
        log.warn("[ACTION] IAM {} request rejected | status={} | departmentId={} | actorId={}",
                operation,
                exception.getStatusCode().value(),
                LogMaskingUtil.maskId(departmentId),
                LogMaskingUtil.maskId(actorId));
        if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
            return new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }
        if (exception.getStatusCode() == HttpStatus.CONFLICT) {
            return new BusinessException(ErrorCode.DEPARTMENT_CONFLICT);
        }
        if (exception.getStatusCode() == HttpStatus.BAD_REQUEST) {
            return new BusinessException(ErrorCode.VAL_001);
        }
        return new BusinessException(ErrorCode.SYS_002);
    }

    private DepartmentRequest toRequest(ManageDepartmentCommand command) {
        return new DepartmentRequest(
                command.actorId(),
                command.code(),
                command.name(),
                command.parentId(),
                command.headUserId(),
                command.glAccountPrefix());
    }

    private record DepartmentRequest(
            UUID actorId,
            String code,
            String name,
            UUID parentId,
            UUID headUserId,
            String glAccountPrefix) {
    }

    private record DeactivateRequest(UUID actorId) {
    }
}
