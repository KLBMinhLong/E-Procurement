package com.eprocure.approval.infrastructure.iam;

import com.eprocure.approval.application.port.out.DelegationResolutionPort;
import com.eprocure.approval.application.port.out.OrgApproverPort;
import com.eprocure.approval.common.api.ApiResponse;
import com.eprocure.approval.common.exception.BusinessException;
import com.eprocure.approval.common.exception.ErrorCode;
import com.eprocure.approval.common.util.LogMaskingUtil;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.eprocure.approval.application.port.out.UserResolverPort;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

@Component
public class IamOrgApproverAdapter implements OrgApproverPort, DelegationResolutionPort, UserResolverPort {
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

    @Override
    public Optional<ActiveDelegation> resolveActiveDelegation(ResolveDelegationQuery query) {
        if (internalApiKey.isBlank()) {
            log.error("[ACTION] Step ResolveActiveDelegationFromIam | delegatorId={} | result=missing_api_key",
                    LogMaskingUtil.maskId(query.delegatorId()));
            return Optional.empty();
        }
        try {
            ApiResponse<IamActiveDelegationResponse> response = iamRestClient.get()
                    .uri(uriBuilder -> buildDelegationUri(uriBuilder, query))
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null || !response.success() || response.data() == null || !response.data().active()) {
                return Optional.empty();
            }
            IamActiveDelegationResponse data = response.data();
            return Optional.of(new ActiveDelegation(
                    data.delegationId(),
                    data.delegatorId(),
                    data.delegateId()));
        } catch (RestClientException exception) {
            log.warn("[ACTION] Step ResolveActiveDelegationFromIam | delegatorId={} | result=unavailable | error={}",
                    LogMaskingUtil.maskId(query.delegatorId()),
                    exception.getMessage());
            return Optional.empty();
        }
    }

    private static java.net.URI buildDelegationUri(UriBuilder uriBuilder, ResolveDelegationQuery query) {
        UriBuilder builder = uriBuilder
                .path("/internal/org/delegations/active")
                .queryParam("delegator_id", query.delegatorId())
                .queryParam("requester_id", query.requesterId())
                .queryParam("requester_department_id", query.requesterDepartmentId())
                .queryParam("total_amount", query.totalAmount())
                .queryParam("currency", query.currency());
        addCategories(builder, query.categories());
        return builder.build();
    }

    private static void addCategories(UriBuilder builder, Set<String> categories) {
        categories.stream()
                .filter(category -> category != null && !category.isBlank())
                .map(String::trim)
                .forEach(category -> builder.queryParam("category", category));
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

    private record IamActiveDelegationResponse(
            boolean active,
            UUID delegationId,
            UUID delegatorId,
            UUID delegateId) {
    }

    @Override
    public Optional<UserSummary> getUserById(UUID userId) {
        if (internalApiKey.isBlank()) {
            log.error("[ACTION] Step ResolveUserFromIam | userId={} | result=missing_api_key", LogMaskingUtil.maskId(userId));
            return Optional.empty();
        }
        try {
            ApiResponse<IamUserDetailResponse> response = iamRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/internal/org/users/{userId}").build(userId))
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || !response.success() || response.data() == null) {
                log.warn("[ACTION] Step ResolveUserFromIam | userId={} | result=empty", LogMaskingUtil.maskId(userId));
                return Optional.empty();
            }

            IamUserDetailResponse data = response.data();
            String deptName = data.department() != null ? data.department().name() : null;
            return Optional.of(new UserSummary(data.id(), data.fullName(), deptName));
        } catch (Exception exception) {
            log.error("[EXCEPTION] IAM user resolution failed | userId={} | error={}",
                    LogMaskingUtil.maskId(userId),
                    exception.getMessage());
            return Optional.empty();
        }
    }

    private record IamUserDetailResponse(
            UUID id,
            String fullName,
            IamDepartmentDetailResponse department) {
    }

    private record IamDepartmentDetailResponse(UUID id, String code, String name) {
    }
}
