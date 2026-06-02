package com.eprocure.vendor.presentation.controller;

import com.eprocure.vendor.application.service.PageResult;
import com.eprocure.vendor.application.service.VendorMutationResult;
import com.eprocure.vendor.application.usecase.ApproveVendorUseCase;
import com.eprocure.vendor.application.usecase.CreateVendorUseCase;
import com.eprocure.vendor.application.usecase.GetVendorDetailUseCase;
import com.eprocure.vendor.application.usecase.ListVendorsUseCase;
import com.eprocure.vendor.common.api.ApiResponse;
import com.eprocure.vendor.common.api.RequestIdUtil;
import com.eprocure.vendor.common.security.UserPrincipal;
import com.eprocure.vendor.common.util.LogMaskingUtil;
import com.eprocure.vendor.domain.model.VendorStatus;
import com.eprocure.vendor.presentation.mapper.VendorPresentationMapper;
import com.eprocure.vendor.presentation.request.ApproveVendorRequest;
import com.eprocure.vendor.presentation.request.CreateVendorRequest;
import com.eprocure.vendor.presentation.response.VendorDetailResponse;
import com.eprocure.vendor.presentation.response.VendorSummaryResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
@RequestMapping("/api/v1/vendors")
public class VendorController {
    private static final Logger log = LogManager.getLogger(VendorController.class);

    private final ListVendorsUseCase listVendorsUseCase;
    private final CreateVendorUseCase createVendorUseCase;
    private final GetVendorDetailUseCase getVendorDetailUseCase;
    private final ApproveVendorUseCase approveVendorUseCase;
    private final VendorPresentationMapper mapper;

    public VendorController(
            ListVendorsUseCase listVendorsUseCase,
            CreateVendorUseCase createVendorUseCase,
            GetVendorDetailUseCase getVendorDetailUseCase,
            ApproveVendorUseCase approveVendorUseCase,
            VendorPresentationMapper mapper) {
        this.listVendorsUseCase = listVendorsUseCase;
        this.createVendorUseCase = createVendorUseCase;
        this.getVendorDetailUseCase = getVendorDetailUseCase;
        this.approveVendorUseCase = approveVendorUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VENDOR_VIEW')")
    public ResponseEntity<ApiResponse<List<VendorSummaryResponse>>> list(
            @RequestParam(value = "status", required = false) VendorStatus status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "on_avl_only", required = false) Boolean onAvlOnly,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", required = false) String sort,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/vendors | userId={}",
                LogMaskingUtil.maskId(principal.getId()));

        PageResult<com.eprocure.vendor.application.service.VendorSummaryView> result = listVendorsUseCase.execute(
                mapper.toListQuery(principal, status, category, onAvlOnly, query, page, size, sort));
        List<VendorSummaryResponse> data = result.items().stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.successWithMeta(data, result.meta(), RequestIdUtil.resolve(request)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('VENDOR_CREATE')")
    public ResponseEntity<ApiResponse<VendorDetailResponse>> create(
            @Valid @RequestBody CreateVendorRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/vendors | userId={}",
                LogMaskingUtil.maskId(principal.getId()));

        VendorMutationResult result = createVendorUseCase.execute(mapper.toCreateCommand(principal, body), idempotencyKey);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.CREATED);
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(mapper.toResponse(result.view()), RequestIdUtil.resolve(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VENDOR_VIEW')")
    public ResponseEntity<ApiResponse<VendorDetailResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/vendors/{} | userId={}",
                LogMaskingUtil.maskId(id),
                LogMaskingUtil.maskId(principal.getId()));

        VendorDetailResponse response = mapper.toResponse(getVendorDetailUseCase.execute(id, principal.getId()));
        return ResponseEntity.ok(ApiResponse.success(response, RequestIdUtil.resolve(request)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('VENDOR_APPROVE')")
    public ResponseEntity<ApiResponse<Void>> approve(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ApproveVendorRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PATCH /api/v1/vendors/{}/approve | userId={}",
                LogMaskingUtil.maskId(id),
                LogMaskingUtil.maskId(principal.getId()));

        VendorMutationResult result = approveVendorUseCase.execute(mapper.toApproveCommand(principal, id, body), idempotencyKey);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.<Void>success(null, RequestIdUtil.resolve(request)));
    }
}
