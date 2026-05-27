package com.eprocure.approval.infrastructure.iam;

import com.eprocure.approval.application.port.out.OrgApproverPort;
import com.eprocure.approval.common.api.ApiResponse;
import com.eprocure.approval.common.exception.BusinessException;
import com.eprocure.approval.common.exception.ErrorCode;
import com.eprocure.approval.common.util.LogMaskingUtil;
import java.util.List;
import java.util.Optional;
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
import org.springframework.web.util.UriBuilder;

@Component
public class IamOrgApproverAdapter implements OrgApproverPort {
    private static final Logger log = LogManager.getLogger(IamOrgApproverAdapter.class);
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient iamRestClient;
    private final String internalApiKey;

    public IamOrgApproverAdapter(
            @Qualifier("iamRestClient") RestClient iamRestClient,
            @Value("${eprocure.integration.iam.internal-api-key:}") String internalApiKey) {
        this.iamRestClient = iamRestClient;
        this.internalApiKey = internalApiKey == null ? "" : internalApiKey;
    }

    @Override
    public List<ApproverCandidate> resolveApprovers(ResolveApproverQuery query) {
        if (internalApiKey.isBlank()) {
            log.error("[ACTION] Step ResolveApproversFromIam | roleCode={} | result=missing_api_key", query.approverRole());
            throw new BusinessException(ErrorCode.APR_002);
        }
        try {
            ApiResponse<List<IamUserSummaryResponse>> response = iamRestClient.get()
                    .uri(uriBuilder -> buildUri(uriBuilder, query))
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || !response.success() || response.data() == null) {
                log.warn("[ACTION] Step ResolveApproversFromIam | roleCode={} | departmentId={} | result=empty",
                        query.approverRole(),
                        LogMaskingUtil.maskId(query.departmentId()));
                throw new BusinessException(ErrorCode.APR_002);
            }

            return response.data().stream()
                    .filter(candidate -> candidate.id() != null)
                    .map(IamOrgApproverAdapter::toCandidate)
                    .toList();
        } catch (RestClientResponseException exception) {
            log.warn("[ACTION] Step ResolveApproversFromIam | roleCode={} | departmentId={} | status={}",
                    query.approverRole(),
                    LogMaskingUtil.maskId(query.departmentId()),
                    exception.getStatusCode().value());
            throw new BusinessException(ErrorCode.APR_002);
        } catch (RestClientException exception) {
            log.error("[EXCEPTION][APR_002] IAM approver resolution failed | roleCode={} | departmentId={} | error={}",
                    query.approverRole(),
                    LogMaskingUtil.maskId(query.departmentId()),
                    exception.getMessage());
            throw new BusinessException(ErrorCode.APR_002);
        }
    }

    private static java.net.URI buildUri(UriBuilder uriBuilder, ResolveApproverQuery query) {
        UriBuilder builder = uriBuilder
                .path("/internal/org/approvers")
                .queryParam("role", query.approverRole())
                .queryParam("department_id", query.departmentId())
                .queryParam("requester_id", query.requesterId());
        return builder.build();
    }

    private static ApproverCandidate toCandidate(IamUserSummaryResponse response) {
        return new ApproverCandidate(
                response.id(),
                response.employeeCode(),
                response.username(),
                response.fullName(),
                response.email(),
                Optional.ofNullable(response.department()).map(IamDepartmentResponse::id).orElse(null));
    }

    private record IamUserSummaryResponse(
            UUID id,
            String employeeCode,
            String username,
            String fullName,
            String email,
            IamDepartmentResponse department) {
    }

    private record IamDepartmentResponse(UUID id) {
    }
}
