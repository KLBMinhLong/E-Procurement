package com.eprocure.finance.presentation.controller;

import com.eprocure.finance.application.service.PageResult;
import com.eprocure.finance.application.usecase.GetBudgetDashboardUseCase;
import com.eprocure.finance.application.usecase.ListBudgetsUseCase;
import com.eprocure.finance.common.api.ApiResponse;
import com.eprocure.finance.common.api.RequestIdUtil;
import com.eprocure.finance.common.security.UserPrincipal;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.BudgetStatus;
import com.eprocure.finance.presentation.mapper.BudgetPresentationMapper;
import com.eprocure.finance.presentation.response.BudgetDashboardResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {
    private static final Logger log = LogManager.getLogger(BudgetController.class);

    private final ListBudgetsUseCase listBudgetsUseCase;
    private final GetBudgetDashboardUseCase getBudgetDashboardUseCase;
    private final BudgetPresentationMapper mapper;

    public BudgetController(
            ListBudgetsUseCase listBudgetsUseCase,
            GetBudgetDashboardUseCase getBudgetDashboardUseCase,
            BudgetPresentationMapper mapper) {
        this.listBudgetsUseCase = listBudgetsUseCase;
        this.getBudgetDashboardUseCase = getBudgetDashboardUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BUDGET_VIEW_OWN_DEPT') or hasAuthority('BUDGET_VIEW_ALL')")
    public ResponseEntity<ApiResponse<List<BudgetDashboardResponse>>> list(
            @RequestParam(value = "department_id", required = false) UUID departmentId,
            @RequestParam(value = "fiscal_year", required = false) Integer fiscalYear,
            @RequestParam(value = "quarter", required = false) Integer quarter,
            @RequestParam(value = "gl_account_code", required = false) String glAccountCode,
            @RequestParam(value = "status", required = false) BudgetStatus status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", required = false) String sort,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/budgets | userId={}",
                LogMaskingUtil.maskId(principal.getId()));

        PageResult<com.eprocure.finance.application.service.BudgetDashboardView> result =
                listBudgetsUseCase.execute(mapper.toListQuery(
                        principal,
                        departmentId,
                        fiscalYear,
                        quarter,
                        glAccountCode,
                        status,
                        page,
                        size,
                        sort));
        List<BudgetDashboardResponse> data = result.items().stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.successWithMeta(data, result.meta(), RequestIdUtil.resolve(request)));
    }

    @GetMapping("/{budgetId}/dashboard")
    @PreAuthorize("hasAuthority('BUDGET_VIEW_OWN_DEPT') or hasAuthority('BUDGET_VIEW_ALL')")
    public ResponseEntity<ApiResponse<BudgetDashboardResponse>> getDashboard(
            @PathVariable UUID budgetId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        log.info("[CONTROLLER] GET /api/v1/budgets/{}/dashboard | userId={}",
                LogMaskingUtil.maskId(budgetId),
                LogMaskingUtil.maskId(principal.getId()));

        BudgetDashboardResponse response = mapper.toResponse(
                getBudgetDashboardUseCase.execute(mapper.toDashboardQuery(principal, budgetId)));
        return ResponseEntity.ok(ApiResponse.success(response, RequestIdUtil.resolve(request)));
    }
}
