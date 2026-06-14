package com.eprocure.inventory.presentation.controller;

import com.eprocure.inventory.application.service.PageResult;
import com.eprocure.inventory.application.usecase.CreateItemUseCase;
import com.eprocure.inventory.application.usecase.GetItemDetailUseCase;
import com.eprocure.inventory.application.usecase.SearchItemsUseCase;
import com.eprocure.inventory.application.usecase.UpdateItemUseCase;
import com.eprocure.inventory.common.api.ApiResponse;
import com.eprocure.inventory.common.api.RequestIdUtil;
import com.eprocure.inventory.common.security.UserPrincipal;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.eprocure.inventory.presentation.mapper.ItemPresentationMapper;
import com.eprocure.inventory.presentation.request.CreateItemRequest;
import com.eprocure.inventory.presentation.request.UpdateItemRequest;
import com.eprocure.inventory.presentation.response.CatalogItemResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
@RequestMapping("/api/v1/items")
public class ItemController {
    private static final Logger log = LogManager.getLogger(ItemController.class);

    private final SearchItemsUseCase searchItemsUseCase;
    private final GetItemDetailUseCase getItemDetailUseCase;
    private final CreateItemUseCase createItemUseCase;
    private final UpdateItemUseCase updateItemUseCase;
    private final ItemPresentationMapper mapper;

    public ItemController(
            SearchItemsUseCase searchItemsUseCase,
            GetItemDetailUseCase getItemDetailUseCase,
            CreateItemUseCase createItemUseCase,
            UpdateItemUseCase updateItemUseCase,
            ItemPresentationMapper mapper) {
        this.searchItemsUseCase = searchItemsUseCase;
        this.getItemDetailUseCase = getItemDetailUseCase;
        this.createItemUseCase = createItemUseCase;
        this.updateItemUseCase = updateItemUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GR_VIEW')")
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> search(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "category_code", required = false) String categoryCode,
            @RequestParam(value = "is_active", required = false) Boolean active,
            @RequestParam(value = "below_reorder", required = false) Boolean belowReorder,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/items | userId={} | page={} | size={}",
                LogMaskingUtil.maskId(principal.getId()),
                page,
                size);
        PageResult<com.eprocure.inventory.application.service.CatalogItemView> result =
                searchItemsUseCase.execute(mapper.toSearchQuery(
                        principal,
                        query,
                        categoryCode,
                        active,
                        belowReorder,
                        page,
                        size));
        List<CatalogItemResponse> data = result.items().stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.successWithMeta(data, result.meta(), RequestIdUtil.resolve(request)));
    }

    @GetMapping("/{itemCode}")
    @PreAuthorize("hasAuthority('GR_VIEW')")
    public ResponseEntity<ApiResponse<CatalogItemResponse>> getByCode(
            @PathVariable String itemCode,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/items/{} | userId={}",
                itemCode,
                LogMaskingUtil.maskId(principal.getId()));
        return ResponseEntity.ok(ApiResponse.success(
                mapper.toResponse(getItemDetailUseCase.execute(mapper.toGetQuery(principal, itemCode))),
                RequestIdUtil.resolve(request)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN_CATALOG_MANAGE')")
    public ResponseEntity<ApiResponse<CatalogItemResponse>> create(
            @Valid @RequestBody CreateItemRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/items | userId={} | itemCode={}",
                LogMaskingUtil.maskId(principal.getId()),
                body.itemCode());
        var result = createItemUseCase.execute(
                mapper.toCreateCommand(principal, body),
                idempotencyKey);
        ResponseEntity.BodyBuilder builder = result.replayed()
                ? ResponseEntity.ok()
                : ResponseEntity.status(HttpStatus.CREATED);
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(mapper.toResponse(result.item()), RequestIdUtil.resolve(request)));
    }

    @PutMapping("/{itemCode}")
    @PreAuthorize("hasAuthority('ADMIN_CATALOG_MANAGE')")
    public ResponseEntity<ApiResponse<CatalogItemResponse>> update(
            @PathVariable String itemCode,
            @Valid @RequestBody UpdateItemRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PUT /api/v1/items/{} | userId={}",
                itemCode,
                LogMaskingUtil.maskId(principal.getId()));
        var result = updateItemUseCase.execute(
                mapper.toUpdateCommand(principal, itemCode, body),
                idempotencyKey);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.replayed()) {
            builder.header("Idempotency-Replayed", "true");
        }
        return builder.body(ApiResponse.success(mapper.toResponse(result.item()), RequestIdUtil.resolve(request)));
    }
}
