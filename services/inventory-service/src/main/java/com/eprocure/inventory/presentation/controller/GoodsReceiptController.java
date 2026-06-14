package com.eprocure.inventory.presentation.controller;

import com.eprocure.inventory.application.service.GoodsReceiptMutationResult;
import com.eprocure.inventory.application.service.PageResult;
import com.eprocure.inventory.application.usecase.CompleteGoodsReceiptUseCase;
import com.eprocure.inventory.application.usecase.CreateGoodsReceiptUseCase;
import com.eprocure.inventory.application.usecase.GetGoodsReceiptUseCase;
import com.eprocure.inventory.application.usecase.ListGoodsReceiptsUseCase;
import com.eprocure.inventory.application.usecase.UpdateGoodsReceiptUseCase;
import com.eprocure.inventory.common.api.ApiResponse;
import com.eprocure.inventory.common.api.RequestIdUtil;
import com.eprocure.inventory.common.security.UserPrincipal;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.eprocure.inventory.domain.model.GoodsReceiptStatus;
import com.eprocure.inventory.presentation.mapper.GoodsReceiptPresentationMapper;
import com.eprocure.inventory.presentation.request.CreateGoodsReceiptRequest;
import com.eprocure.inventory.presentation.request.UpdateGoodsReceiptRequest;
import com.eprocure.inventory.presentation.response.CompleteGoodsReceiptResponse;
import com.eprocure.inventory.presentation.response.GoodsReceiptResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goods-receipts")
public class GoodsReceiptController {
    private static final Logger log = LogManager.getLogger(GoodsReceiptController.class);

    private final ListGoodsReceiptsUseCase listGoodsReceiptsUseCase;
    private final CreateGoodsReceiptUseCase createGoodsReceiptUseCase;
    private final GetGoodsReceiptUseCase getGoodsReceiptUseCase;
    private final UpdateGoodsReceiptUseCase updateGoodsReceiptUseCase;
    private final CompleteGoodsReceiptUseCase completeGoodsReceiptUseCase;
    private final GoodsReceiptPresentationMapper mapper;

    public GoodsReceiptController(
            ListGoodsReceiptsUseCase listGoodsReceiptsUseCase,
            CreateGoodsReceiptUseCase createGoodsReceiptUseCase,
            GetGoodsReceiptUseCase getGoodsReceiptUseCase,
            UpdateGoodsReceiptUseCase updateGoodsReceiptUseCase,
            CompleteGoodsReceiptUseCase completeGoodsReceiptUseCase,
            GoodsReceiptPresentationMapper mapper) {
        this.listGoodsReceiptsUseCase = listGoodsReceiptsUseCase;
        this.createGoodsReceiptUseCase = createGoodsReceiptUseCase;
        this.getGoodsReceiptUseCase = getGoodsReceiptUseCase;
        this.updateGoodsReceiptUseCase = updateGoodsReceiptUseCase;
        this.completeGoodsReceiptUseCase = completeGoodsReceiptUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GR_VIEW')")
    public ResponseEntity<ApiResponse<List<GoodsReceiptResponse>>> list(
            @RequestParam(value = "status", required = false) GoodsReceiptStatus status,
            @RequestParam(value = "po_id", required = false) UUID poId,
            @RequestParam(value = "warehouse_id", required = false) UUID warehouseId,
            @RequestParam(value = "from_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "to_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/goods-receipts | userId={}",
                LogMaskingUtil.maskId(principal.getId()));
        PageResult<com.eprocure.inventory.application.service.GoodsReceiptView> result =
                listGoodsReceiptsUseCase.execute(mapper.toListQuery(
                        principal,
                        status,
                        poId,
                        warehouseId,
                        fromDate,
                        toDate,
                        page,
                        size));
        List<GoodsReceiptResponse> data = result.items().stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.successWithMeta(data, result.meta(), RequestIdUtil.resolve(request)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GR_CREATE')")
    public ResponseEntity<ApiResponse<GoodsReceiptResponse>> create(
            @Valid @RequestBody CreateGoodsReceiptRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/goods-receipts | userId={} | poId={}",
                LogMaskingUtil.maskId(principal.getId()),
                LogMaskingUtil.maskId(body.poId()));
        GoodsReceiptMutationResult result = createGoodsReceiptUseCase.execute(
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

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('GR_VIEW')")
    public ResponseEntity<ApiResponse<GoodsReceiptResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/goods-receipts/{} | userId={}",
                LogMaskingUtil.maskId(id),
                LogMaskingUtil.maskId(principal.getId()));
        GoodsReceiptResponse response = mapper.toResponse(
                getGoodsReceiptUseCase.execute(mapper.toGetQuery(principal, id)));
        return ResponseEntity.ok(ApiResponse.success(response, RequestIdUtil.resolve(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GR_CREATE')")
    public ResponseEntity<ApiResponse<GoodsReceiptResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGoodsReceiptRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PUT /api/v1/goods-receipts/{} | userId={}",
                LogMaskingUtil.maskId(id),
                LogMaskingUtil.maskId(principal.getId()));
        GoodsReceiptMutationResult result = updateGoodsReceiptUseCase.execute(
                mapper.toUpdateCommand(principal, id, body),
                idempotencyKey);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(mapper.toResponse(result.view()), RequestIdUtil.resolve(request)));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('GR_CREATE')")
    public ResponseEntity<ApiResponse<CompleteGoodsReceiptResponse>> complete(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/goods-receipts/{}/complete | userId={}",
                LogMaskingUtil.maskId(id),
                LogMaskingUtil.maskId(principal.getId()));
        var result = completeGoodsReceiptUseCase.execute(
                mapper.toCompleteCommand(principal, id),
                idempotencyKey);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(mapper.toResponse(result), RequestIdUtil.resolve(request)));
    }
}
