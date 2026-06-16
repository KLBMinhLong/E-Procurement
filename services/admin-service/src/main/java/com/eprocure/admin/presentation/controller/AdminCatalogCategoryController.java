package com.eprocure.admin.presentation.controller;

import com.eprocure.admin.application.usecase.CreateCatalogCategoryUseCase;
import com.eprocure.admin.application.usecase.DeactivateCatalogCategoryUseCase;
import com.eprocure.admin.application.usecase.ListCatalogCategoriesUseCase;
import com.eprocure.admin.application.usecase.UpdateCatalogCategoryUseCase;
import com.eprocure.admin.common.api.ApiResponse;
import com.eprocure.admin.common.api.RequestIdUtil;
import com.eprocure.admin.common.security.UserPrincipal;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.presentation.mapper.AdminCatalogPresentationMapper;
import com.eprocure.admin.presentation.request.CatalogCategoryRequest;
import com.eprocure.admin.presentation.response.CatalogCategoryResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/catalog/categories")
public class AdminCatalogCategoryController {
    private static final Logger log = LogManager.getLogger(AdminCatalogCategoryController.class);

    private final ListCatalogCategoriesUseCase listUseCase;
    private final CreateCatalogCategoryUseCase createUseCase;
    private final UpdateCatalogCategoryUseCase updateUseCase;
    private final DeactivateCatalogCategoryUseCase deactivateUseCase;
    private final AdminCatalogPresentationMapper mapper;

    public AdminCatalogCategoryController(
            ListCatalogCategoriesUseCase listUseCase,
            CreateCatalogCategoryUseCase createUseCase,
            UpdateCatalogCategoryUseCase updateUseCase,
            DeactivateCatalogCategoryUseCase deactivateUseCase,
            AdminCatalogPresentationMapper mapper) {
        this.listUseCase = listUseCase;
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deactivateUseCase = deactivateUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN_CATALOG_MANAGE')")
    public ResponseEntity<ApiResponse<List<CatalogCategoryResponse>>> listCategories(
            @RequestParam(name = "include_inactive", defaultValue = "false") boolean includeInactive,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/admin/catalog/categories | userId={} | includeInactive={}",
                LogMaskingUtil.maskId(principal.getId()),
                includeInactive);
        return ResponseEntity.ok(ApiResponse.success(
                mapper.toResponseList(listUseCase.execute(includeInactive)),
                RequestIdUtil.resolve(request)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN_CATALOG_MANAGE')")
    public ResponseEntity<ApiResponse<CatalogCategoryResponse>> createCategory(
            @Valid @RequestBody CatalogCategoryRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] POST /api/v1/admin/catalog/categories | userId={} | code={}",
                LogMaskingUtil.maskId(principal.getId()),
                body.code());
        String requestId = RequestIdUtil.resolve(request);
        var view = createUseCase.execute(
                mapper.toCreateCommand(body, principal, mapper.toAuditContext(principal, request, requestId)),
                idempotencyKey);
        return ResponseEntity.created(URI.create("/api/v1/admin/catalog/categories/" + view.code()))
                .body(ApiResponse.success(mapper.toResponse(view), requestId));
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasAuthority('ADMIN_CATALOG_MANAGE')")
    public ResponseEntity<ApiResponse<CatalogCategoryResponse>> updateCategory(
            @PathVariable String code,
            @Valid @RequestBody CatalogCategoryRequest body,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PUT /api/v1/admin/catalog/categories/{} | userId={}",
                code,
                LogMaskingUtil.maskId(principal.getId()));
        String requestId = RequestIdUtil.resolve(request);
        var view = updateUseCase.execute(
                mapper.toUpdateCommand(code, body, principal, mapper.toAuditContext(principal, request, requestId)),
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(view), requestId));
    }

    @PatchMapping("/{code}/deactivate")
    @PreAuthorize("hasAuthority('ADMIN_CATALOG_MANAGE')")
    public ResponseEntity<ApiResponse<CatalogCategoryResponse>> deactivateCategory(
            @PathVariable String code,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] PATCH /api/v1/admin/catalog/categories/{}/deactivate | userId={}",
                code,
                LogMaskingUtil.maskId(principal.getId()));
        String requestId = RequestIdUtil.resolve(request);
        var view = deactivateUseCase.execute(
                mapper.toDeactivateCommand(code, principal, mapper.toAuditContext(principal, request, requestId)),
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(view), requestId));
    }
}
