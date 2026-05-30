package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.GetBudgetDashboardQuery;
import com.eprocure.finance.application.port.out.BudgetDashboardCachePort;
import com.eprocure.finance.application.service.BudgetDashboardView;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.BudgetLedgerSummary;
import com.eprocure.finance.domain.repository.BudgetRepository;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetBudgetDashboardUseCase {
    private static final Logger log = LogManager.getLogger(GetBudgetDashboardUseCase.class);
    private static final String PERMISSION_VIEW_ALL = "BUDGET_VIEW_ALL";
    private static final String PERMISSION_VIEW_OWN_DEPT = "BUDGET_VIEW_OWN_DEPT";

    private final BudgetRepository budgetRepository;
    private final BudgetDashboardCachePort cachePort;

    public GetBudgetDashboardUseCase(BudgetRepository budgetRepository, BudgetDashboardCachePort cachePort) {
        this.budgetRepository = budgetRepository;
        this.cachePort = cachePort;
    }

    @Transactional(readOnly = true)
    public BudgetDashboardView execute(GetBudgetDashboardQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        log.info("[ACTION] Start GetBudgetDashboard | userId={} | budgetId={}",
                LogMaskingUtil.maskId(query.actorId()),
                LogMaskingUtil.maskId(query.budgetId()));

        BudgetDashboardView dashboard = cachePort.findByBudgetId(query.budgetId())
                .orElseGet(() -> loadAndCache(query.budgetId()));
        verifyScope(query, dashboard.departmentId());

        log.info("[ACTION] Complete GetBudgetDashboard | userId={} | budgetId={}",
                LogMaskingUtil.maskId(query.actorId()),
                LogMaskingUtil.maskId(query.budgetId()));
        return dashboard;
    }

    private BudgetDashboardView loadAndCache(UUID budgetId) {
        BudgetLedgerSummary summary = budgetRepository.findSummaryById(budgetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIN_001));
        BudgetDashboardView dashboard = BudgetDashboardView.from(summary);
        cachePort.store(dashboard);
        return dashboard;
    }

    private void verifyScope(GetBudgetDashboardQuery query, UUID budgetDepartmentId) {
        if (query.hasPermission(PERMISSION_VIEW_ALL)) {
            return;
        }
        if (query.hasPermission(PERMISSION_VIEW_OWN_DEPT)
                && query.actorDepartmentId().equals(budgetDepartmentId)) {
            return;
        }
        throw new BusinessException(ErrorCode.IAM_004);
    }
}
