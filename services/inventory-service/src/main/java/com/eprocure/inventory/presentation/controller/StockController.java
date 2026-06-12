package com.eprocure.inventory.presentation.controller;

import com.eprocure.inventory.application.usecase.AdjustStockUseCase;
import com.eprocure.inventory.application.service.PageResult;
import com.eprocure.inventory.application.usecase.GetItemStockUseCase;
import com.eprocure.inventory.application.usecase.IssueOutStockUseCase;
import com.eprocure.inventory.application.usecase.ListStockMovementsUseCase;
import com.eprocure.inventory.application.usecase.ListWarehousesUseCase;
import com.eprocure.inventory.application.usecase.ListWarehouseStockUseCase;
import com.eprocure.inventory.common.api.ApiResponse;
import com.eprocure.inventory.common.api.RequestIdUtil;
import com.eprocure.inventory.common.security.UserPrincipal;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.eprocure.inventory.domain.model.StockMovementType;
import com.eprocure.inventory.presentation.mapper.StockPresentationMapper;
import com.eprocure.inventory.presentation.request.AdjustStockRequest;
import com.eprocure.inventory.presentation.request.IssueOutStockRequest;
import com.eprocure.inventory.presentation.response.IssueOutStockResponse;
import com.eprocure.inventory.presentation.response.StockEntryResponse;
import com.eprocure.inventory.presentation.response.StockMovementResponse;
import com.eprocure.inventory.presentation.response.WarehouseListResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1")
public class StockController {
    private static final Logger log = LogManager.getLogger(StockController.class);

    private final GetItemStockUseCase getItemStockUseCase;
    private final ListWarehousesUseCase listWarehousesUseCase;
    private final ListWarehouseStockUseCase listWarehouseStockUseCase;
    private final ListStockMovementsUseCase listStockMovementsUseCase;
    private final IssueOutStockUseCase issueOutStockUseCase;
    private final AdjustStockUseCase adjustStockUseCase;
    private final StockPresentationMapper mapper;

    public StockController(
            GetItemStockUseCase getItemStockUseCase,
            ListWarehousesUseCase listWarehousesUseCase,
            ListWarehouseStockUseCase listWarehouseStockUseCase,
            ListStockMovementsUseCase listStockMovementsUseCase,
            IssueOutStockUseCase issueOutStockUseCase,
            AdjustStockUseCase adjustStockUseCase,
            StockPresentationMapper mapper) {
        this.getItemStockUseCase = getItemStockUseCase;
        this.listWarehousesUseCase = listWarehousesUseCase;
        this.listWarehouseStockUseCase = listWarehouseStockUseCase;
        this.listStockMovementsUseCase = listStockMovementsUseCase;
        this.issueOutStockUseCase = issueOutStockUseCase;
        this.adjustStockUseCase = adjustStockUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/warehouses")
    @PreAuthorize("hasAuthority('GR_VIEW')")
    public ResponseEntity<ApiResponse<List<WarehouseListResponse>>> listWarehouses(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/warehouses | userId={}",
                LogMaskingUtil.maskId(principal.getId()));
        return ResponseEntity.ok(ApiResponse.success(
                listWarehousesUseCase.execute(),
                RequestIdUtil.resolve(request)));
    }

    @GetMapping("/items/{itemCode}/stock")
    @PreAuthorize("hasAuthority('GR_VIEW')")
    public ResponseEntity<ApiResponse<List<StockEntryResponse>>> getItemStock(
            @PathVariable String itemCode,
            @RequestParam(value = "warehouse_id", required = false) UUID warehouseId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/items/{}/stock | userId={} | warehouseId={}",
                itemCode,
                LogMaskingUtil.maskId(principal.getId()),
                LogMaskingUtil.maskId(warehouseId));
        var result = getItemStockUseCase.execute(mapper.toGetItemStockQuery(
                principal,
                itemCode,
                warehouseId));
        List<StockEntryResponse> data = result.stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data, RequestIdUtil.resolve(request)));
    }

    @GetMapping("/warehouses/{id}/stock")
    @PreAuthorize("hasAuthority('GR_VIEW')")
    public ResponseEntity<ApiResponse<List<StockEntryResponse>>> listWarehouseStock(
            @PathVariable UUID id,
            @RequestParam(value = "below_reorder", required = false) Boolean belowReorder,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/warehouses/{}/stock | userId={} | belowReorder={}",
                LogMaskingUtil.maskId(id),
                LogMaskingUtil.maskId(principal.getId()),
                belowReorder);
        PageResult<com.eprocure.inventory.application.service.StockEntryView> result =
                listWarehouseStockUseCase.execute(mapper.toListWarehouseStockQuery(
                        principal,
                        id,
                        belowReorder,
                        page,
                        size));
        List<StockEntryResponse> data = result.items().stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.successWithMeta(data, result.meta(), RequestIdUtil.resolve(request)));
    }

    @GetMapping("/stock/movements")
    @PreAuthorize("hasAuthority('GR_VIEW')")
    public ResponseEntity<ApiResponse<List<StockMovementResponse>>> listStockMovements(
            @RequestParam(value = "item_code", required = false) String itemCode,
            @RequestParam(value = "warehouse_id", required = false) UUID warehouseId,
            @RequestParam(value = "movement_type", required = false) StockMovementType movementType,
            @RequestParam(value = "from_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "to_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/stock/movements | userId={} | page={} | size={}",
                LogMaskingUtil.maskId(principal.getId()),
                page,
                size);
        PageResult<com.eprocure.inventory.application.service.StockMovementView> result =
                listStockMovementsUseCase.execute(mapper.toListStockMovementsQuery(
                        principal,
                        itemCode,
                        warehouseId,
                        movementType,
                        fromDate,
                        toDate,
                        page,
                        size));
        List<StockMovementResponse> data = result.items().stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.successWithMeta(data, result.meta(), RequestIdUtil.resolve(request)));
    }

    @PostMapping("/stock/issue-out")
    @PreAuthorize("hasAuthority('GR_ISSUE_OUT')")
    public ResponseEntity<ApiResponse<IssueOutStockResponse>> issueOut(
            @Valid @RequestBody IssueOutStockRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/stock/issue-out | userId={} | warehouseId={} | lineCount={}",
                LogMaskingUtil.maskId(principal.getId()),
                LogMaskingUtil.maskId(body.warehouseId()),
                body.items().size());
        var result = issueOutStockUseCase.execute(
                mapper.toIssueOutCommand(principal, body),
                idempotencyKey);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(mapper.toResponse(result), RequestIdUtil.resolve(request)));
    }

    @PostMapping("/stock/adjustment")
    @PreAuthorize("hasAuthority('ADMIN_CATALOG_MANAGE')")
    public ResponseEntity<ApiResponse<StockMovementResponse>> adjustStock(
            @Valid @RequestBody AdjustStockRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/stock/adjustment | userId={} | warehouseId={} | itemCode={}",
                LogMaskingUtil.maskId(principal.getId()),
                LogMaskingUtil.maskId(body.warehouseId()),
                body.itemCode());
        var result = adjustStockUseCase.execute(
                mapper.toAdjustStockCommand(principal, body),
                idempotencyKey);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(mapper.toResponse(result.movement()), RequestIdUtil.resolve(request)));
    }
}
