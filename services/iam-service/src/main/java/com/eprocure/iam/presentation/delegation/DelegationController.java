package com.eprocure.iam.presentation.delegation;

import com.eprocure.iam.application.port.in.CreateDelegationCommand;
import com.eprocure.iam.application.port.in.RevokeDelegationCommand;
import com.eprocure.iam.application.service.DelegationDetailView;
import com.eprocure.iam.application.usecase.CreateDelegationUseCase;
import com.eprocure.iam.application.usecase.ListMyDelegationsUseCase;
import com.eprocure.iam.application.usecase.RevokeDelegationUseCase;
import com.eprocure.iam.common.api.ApiResponse;
import com.eprocure.iam.common.api.RequestIdUtil;
import com.eprocure.iam.common.security.UserPrincipal;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.DelegationStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delegations")
public class DelegationController {
    private static final Logger log = LogManager.getLogger(DelegationController.class);
    private final ListMyDelegationsUseCase listMyDelegationsUseCase;
    private final CreateDelegationUseCase createDelegationUseCase;
    private final RevokeDelegationUseCase revokeDelegationUseCase;

    public DelegationController(
            ListMyDelegationsUseCase listMyDelegationsUseCase,
            CreateDelegationUseCase createDelegationUseCase,
            RevokeDelegationUseCase revokeDelegationUseCase) {
        this.listMyDelegationsUseCase = listMyDelegationsUseCase;
        this.createDelegationUseCase = createDelegationUseCase;
        this.revokeDelegationUseCase = revokeDelegationUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DELEGATION_MANAGE')")
    public ResponseEntity<ApiResponse<List<DelegationDetailView>>> listMyDelegations(
            @RequestParam(required = false) DelegationStatus status,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/delegations | userId={} | status={}",
                LogMaskingUtil.maskId(principal.getId()),
                status);
        return ResponseEntity.ok(ApiResponse.success(
                listMyDelegationsUseCase.execute(principal.getId(), status),
                RequestIdUtil.resolve(request)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DELEGATION_MANAGE')")
    public ResponseEntity<ApiResponse<DelegationDetailView>> createDelegation(
            @Valid @RequestBody DelegationRequest body,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/delegations | userId={} | delegateId={}",
                LogMaskingUtil.maskId(principal.getId()),
                LogMaskingUtil.maskId(body.delegateId()));
        DelegationDetailView view = createDelegationUseCase.execute(
                new CreateDelegationCommand(
                        principal.getId(),
                        body.delegateId(),
                        body.startAt(),
                        body.endAt(),
                        body.maxValue(),
                        body.currency(),
                        body.allowedCategories(),
                        body.scope()),
                idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(view, RequestIdUtil.resolve(request)));
    }

    @PatchMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('DELEGATION_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> revokeDelegation(
            @PathVariable UUID id,
            @Valid @RequestBody RevokeDelegationRequest body,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PATCH /api/v1/delegations/{}/revoke | userId={}",
                LogMaskingUtil.maskId(id),
                LogMaskingUtil.maskId(principal.getId()));
        revokeDelegationUseCase.execute(
                new RevokeDelegationCommand(principal.getId(), id, body.reason()),
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.successMessage("Delegation revoked", RequestIdUtil.resolve(request)));
    }
}
