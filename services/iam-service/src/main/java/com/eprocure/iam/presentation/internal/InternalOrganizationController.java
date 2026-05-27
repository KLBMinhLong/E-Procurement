package com.eprocure.iam.presentation.internal;

import com.eprocure.iam.application.port.in.ResolveApproversQuery;
import com.eprocure.iam.application.service.InternalApiKeyGuard;
import com.eprocure.iam.application.service.UserSummaryView;
import com.eprocure.iam.application.usecase.ResolveApproversUseCase;
import com.eprocure.iam.common.api.ApiResponse;
import com.eprocure.iam.common.api.RequestIdUtil;
import com.eprocure.iam.common.util.LogMaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/org")
public class InternalOrganizationController {
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final Logger log = LogManager.getLogger(InternalOrganizationController.class);

    private final InternalApiKeyGuard internalApiKeyGuard;
    private final ResolveApproversUseCase resolveApproversUseCase;
    private final com.eprocure.iam.application.usecase.GetUserByIdUseCase getUserByIdUseCase;

    public InternalOrganizationController(
            InternalApiKeyGuard internalApiKeyGuard,
            ResolveApproversUseCase resolveApproversUseCase,
            com.eprocure.iam.application.usecase.GetUserByIdUseCase getUserByIdUseCase) {
        this.internalApiKeyGuard = internalApiKeyGuard;
        this.resolveApproversUseCase = resolveApproversUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
    }

    @GetMapping("/approvers")
    public ResponseEntity<ApiResponse<List<UserSummaryView>>> getApprovers(
            @RequestHeader(INTERNAL_API_KEY_HEADER) String internalApiKey,
            @RequestParam(name = "role") String roleCode,
            @RequestParam(name = "department_id") UUID departmentId,
            @RequestParam(name = "requester_id") UUID requesterId,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] GET /internal/org/approvers | userId=internal | roleCode={} | departmentId={} | requesterId={}",
                roleCode,
                LogMaskingUtil.maskId(departmentId),
                LogMaskingUtil.maskId(requesterId));
        return ResponseEntity.ok(ApiResponse.success(
                resolveApproversUseCase.execute(new ResolveApproversQuery(roleCode, departmentId, requesterId)),
                RequestIdUtil.resolve(request)));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<com.eprocure.iam.application.service.UserDetailView>> getUserById(
            @RequestHeader(INTERNAL_API_KEY_HEADER) String internalApiKey,
            @PathVariable UUID userId,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] GET /internal/org/users/{} | userId=internal", LogMaskingUtil.maskId(userId));
        return ResponseEntity.ok(ApiResponse.success(
                getUserByIdUseCase.execute(userId),
                RequestIdUtil.resolve(request)));
    }
}
