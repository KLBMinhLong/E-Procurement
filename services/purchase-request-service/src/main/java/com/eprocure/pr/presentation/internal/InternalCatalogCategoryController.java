package com.eprocure.pr.presentation.internal;

import com.eprocure.pr.application.port.in.DeactivateCatalogCategoryCommand;
import com.eprocure.pr.application.port.in.ManageCatalogCategoryCommand;
import com.eprocure.pr.application.service.CatalogCategoryAdminView;
import com.eprocure.pr.application.service.InternalApiKeyGuard;
import com.eprocure.pr.application.usecase.CreateCatalogCategoryUseCase;
import com.eprocure.pr.application.usecase.DeactivateCatalogCategoryUseCase;
import com.eprocure.pr.application.usecase.ListAdminCatalogCategoriesUseCase;
import com.eprocure.pr.application.usecase.UpdateCatalogCategoryUseCase;
import com.eprocure.pr.common.api.ApiResponse;
import com.eprocure.pr.common.api.RequestIdUtil;
import com.eprocure.pr.common.util.LogMaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/internal/catalog/categories")
public class InternalCatalogCategoryController {
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final Logger log = LogManager.getLogger(InternalCatalogCategoryController.class);

    private final InternalApiKeyGuard internalApiKeyGuard;
    private final ListAdminCatalogCategoriesUseCase listUseCase;
    private final CreateCatalogCategoryUseCase createUseCase;
    private final UpdateCatalogCategoryUseCase updateUseCase;
    private final DeactivateCatalogCategoryUseCase deactivateUseCase;

    public InternalCatalogCategoryController(
            InternalApiKeyGuard internalApiKeyGuard,
            ListAdminCatalogCategoriesUseCase listUseCase,
            CreateCatalogCategoryUseCase createUseCase,
            UpdateCatalogCategoryUseCase updateUseCase,
            DeactivateCatalogCategoryUseCase deactivateUseCase) {
        this.internalApiKeyGuard = internalApiKeyGuard;
        this.listUseCase = listUseCase;
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deactivateUseCase = deactivateUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CatalogCategoryAdminView>>> listCategories(
            @RequestHeader(value = INTERNAL_API_KEY_HEADER, required = false) String internalApiKey,
            @RequestParam(name = "include_inactive", defaultValue = "false") boolean includeInactive,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] GET /internal/catalog/categories | userId=internal | includeInactive={}",
                includeInactive);
        return ResponseEntity.ok(ApiResponse.success(listUseCase.execute(includeInactive), RequestIdUtil.resolve(request)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CatalogCategoryAdminView>> createCategory(
            @RequestHeader(value = INTERNAL_API_KEY_HEADER, required = false) String internalApiKey,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody InternalCatalogCategoryRequest body,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] POST /internal/catalog/categories | userId=internal | actorId={} | code={}",
                LogMaskingUtil.maskId(body.actorId()),
                body.code());
        CatalogCategoryAdminView view = createUseCase.execute(toCommand(body.code(), body), idempotencyKey);
        return ResponseEntity.created(URI.create("/internal/catalog/categories/" + view.code()))
                .body(ApiResponse.success(view, RequestIdUtil.resolve(request)));
    }

    @PutMapping("/{code}")
    public ResponseEntity<ApiResponse<CatalogCategoryAdminView>> updateCategory(
            @RequestHeader(value = INTERNAL_API_KEY_HEADER, required = false) String internalApiKey,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable String code,
            @Valid @RequestBody InternalCatalogCategoryRequest body,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] PUT /internal/catalog/categories/{} | userId=internal | actorId={}",
                code,
                LogMaskingUtil.maskId(body.actorId()));
        CatalogCategoryAdminView view = updateUseCase.execute(toCommand(code, body), idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(view, RequestIdUtil.resolve(request)));
    }

    @PatchMapping("/{code}/deactivate")
    public ResponseEntity<ApiResponse<CatalogCategoryAdminView>> deactivateCategory(
            @RequestHeader(value = INTERNAL_API_KEY_HEADER, required = false) String internalApiKey,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable String code,
            @Valid @RequestBody InternalCatalogCategoryDeactivateRequest body,
            HttpServletRequest request) {
        internalApiKeyGuard.verify(internalApiKey);
        log.info("[CONTROLLER] PATCH /internal/catalog/categories/{}/deactivate | userId=internal | actorId={}",
                code,
                LogMaskingUtil.maskId(body.actorId()));
        CatalogCategoryAdminView view = deactivateUseCase.execute(
                new DeactivateCatalogCategoryCommand(body.actorId(), code),
                idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(view, RequestIdUtil.resolve(request)));
    }

    private ManageCatalogCategoryCommand toCommand(String code, InternalCatalogCategoryRequest body) {
        return new ManageCatalogCategoryCommand(
                body.actorId(),
                code,
                body.name(),
                body.parentCode(),
                body.requiresSpecialApproval(),
                body.specialApproverRole(),
                body.requiresRfqAbove(),
                Boolean.TRUE.equals(body.isCapex()));
    }
}
