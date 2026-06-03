package com.eprocure.finance.presentation.controller;

import com.eprocure.finance.application.service.InvoiceMutationResult;
import com.eprocure.finance.application.service.PageResult;
import com.eprocure.finance.application.usecase.CreateInvoiceUseCase;
import com.eprocure.finance.application.usecase.GetInvoiceUseCase;
import com.eprocure.finance.application.usecase.ListInvoicesUseCase;
import com.eprocure.finance.common.api.ApiResponse;
import com.eprocure.finance.common.api.RequestIdUtil;
import com.eprocure.finance.common.security.UserPrincipal;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.InvoiceStatus;
import com.eprocure.finance.presentation.mapper.InvoicePresentationMapper;
import com.eprocure.finance.presentation.request.CreateInvoiceRequest;
import com.eprocure.finance.presentation.response.InvoiceResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {
    private static final Logger log = LogManager.getLogger(InvoiceController.class);

    private final ListInvoicesUseCase listInvoicesUseCase;
    private final GetInvoiceUseCase getInvoiceUseCase;
    private final CreateInvoiceUseCase createInvoiceUseCase;
    private final InvoicePresentationMapper mapper;

    public InvoiceController(
            ListInvoicesUseCase listInvoicesUseCase,
            GetInvoiceUseCase getInvoiceUseCase,
            CreateInvoiceUseCase createInvoiceUseCase,
            InvoicePresentationMapper mapper) {
        this.listInvoicesUseCase = listInvoicesUseCase;
        this.getInvoiceUseCase = getInvoiceUseCase;
        this.createInvoiceUseCase = createInvoiceUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INVOICE_VIEW')")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> list(
            @RequestParam(value = "status", required = false) InvoiceStatus status,
            @RequestParam(value = "vendor_id", required = false) UUID vendorId,
            @RequestParam(value = "po_id", required = false) UUID poId,
            @RequestParam(value = "overdue_only", required = false) Boolean overdueOnly,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/invoices | userId={}", LogMaskingUtil.maskId(principal.getId()));
        PageResult<com.eprocure.finance.application.service.InvoiceView> result =
                listInvoicesUseCase.execute(mapper.toListQuery(
                        principal,
                        status,
                        vendorId,
                        poId,
                        overdueOnly,
                        page,
                        size));
        List<InvoiceResponse> data = result.items().stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.successWithMeta(data, result.meta(), RequestIdUtil.resolve(request)));
    }

    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAuthority('INVOICE_VIEW')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> get(
            @PathVariable UUID invoiceId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/invoices/{} | userId={}",
                LogMaskingUtil.maskId(invoiceId),
                LogMaskingUtil.maskId(principal.getId()));
        InvoiceResponse response = mapper.toResponse(
                getInvoiceUseCase.execute(mapper.toGetQuery(principal, invoiceId)));
        return ResponseEntity.ok(ApiResponse.success(response, RequestIdUtil.resolve(request)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVOICE_CREATE')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> create(
            @Valid @RequestBody CreateInvoiceRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/invoices | userId={} | poId={}",
                LogMaskingUtil.maskId(principal.getId()),
                LogMaskingUtil.maskId(body.poId()));
        InvoiceMutationResult result = createInvoiceUseCase.execute(
                mapper.toCreateCommand(principal, body),
                idempotencyKey);
        ResponseEntity.BodyBuilder builder = result.replayed()
                ? ResponseEntity.ok()
                : ResponseEntity.status(HttpStatus.CREATED);
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(mapper.toResponse(result.view()), RequestIdUtil.resolve(request)));
    }
}
