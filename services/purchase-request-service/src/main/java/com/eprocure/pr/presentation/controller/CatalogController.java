package com.eprocure.pr.presentation.controller;

import com.eprocure.pr.application.port.in.SearchCatalogItemsQuery;
import com.eprocure.pr.application.usecase.GetCatalogCategoriesUseCase;
import com.eprocure.pr.application.usecase.SearchCatalogItemsUseCase;
import com.eprocure.pr.common.api.ApiResponse;
import com.eprocure.pr.common.api.RequestIdUtil;
import com.eprocure.pr.common.security.UserPrincipal;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.domain.model.CatalogCategory;
import com.eprocure.pr.domain.model.CatalogItem;
import com.eprocure.pr.presentation.response.CatalogCategoryResponse;
import com.eprocure.pr.presentation.response.CatalogItemResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {
    private static final Logger log = LogManager.getLogger(CatalogController.class);

    private final GetCatalogCategoriesUseCase getCategoriesUseCase;
    private final SearchCatalogItemsUseCase searchItemsUseCase;

    public CatalogController(GetCatalogCategoriesUseCase getCategoriesUseCase, SearchCatalogItemsUseCase searchItemsUseCase) {
        this.getCategoriesUseCase = getCategoriesUseCase;
        this.searchItemsUseCase = searchItemsUseCase;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CatalogCategoryResponse>>> getCategories(HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/catalog/categories");

        List<CatalogCategory> categories = getCategoriesUseCase.getCategories();
        List<CatalogCategoryResponse> response = categories.stream()
                .map(CatalogCategoryResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response, RequestIdUtil.resolve(request)));
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> searchItems(
            @RequestParam(required = false) String q,
            @RequestParam(name = "category_code", required = false) String categoryCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        log.info("[CONTROLLER] GET /api/v1/catalog/items | userId={} | q={} | category={}",
                LogMaskingUtil.maskId(principal.getId()), q, categoryCode);

        SearchCatalogItemsQuery query = new SearchCatalogItemsQuery(principal.getId(), q, categoryCode, page, size);
        SearchCatalogItemsUseCase.PagedResult<CatalogItem> result = searchItemsUseCase.search(query);

        List<CatalogItemResponse> response = result.content().stream()
                .map(CatalogItemResponse::from)
                .collect(Collectors.toList());

        Map<String, Object> meta = Map.of(
                "page", result.page(),
                "size", result.size(),
                "totalElements", result.totalElements(),
                "totalPages", result.totalPages(),
                "isFirst", result.isFirst(),
                "isLast", result.isLast()
        );

        return ResponseEntity.ok(ApiResponse.successWithMeta(response, meta, RequestIdUtil.resolve(request)));
    }
}
