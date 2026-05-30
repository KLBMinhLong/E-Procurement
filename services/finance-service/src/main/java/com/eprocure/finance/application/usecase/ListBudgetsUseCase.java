package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.ListBudgetsQuery;
import com.eprocure.finance.application.service.BudgetDashboardView;
import com.eprocure.finance.application.service.PageMeta;
import com.eprocure.finance.application.service.PageResult;
import com.eprocure.finance.common.exception.BusinessException;
import com.eprocure.finance.common.exception.ErrorCode;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.BudgetLedgerSummary;
import com.eprocure.finance.domain.repository.BudgetFilter;
import com.eprocure.finance.domain.repository.BudgetRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListBudgetsUseCase {
    private static final Logger log = LogManager.getLogger(ListBudgetsUseCase.class);
    private static final String PERMISSION_VIEW_ALL = "BUDGET_VIEW_ALL";
    private static final String PERMISSION_VIEW_OWN_DEPT = "BUDGET_VIEW_OWN_DEPT";

    private final BudgetRepository budgetRepository;

    public ListBudgetsUseCase(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<BudgetDashboardView> execute(ListBudgetsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        BudgetSort sort = BudgetSort.from(query.sort());
        UUID effectiveDepartmentId = effectiveDepartmentId(query);
        if (effectiveDepartmentId == null && !query.hasPermission(PERMISSION_VIEW_ALL)) {
            throw new BusinessException(ErrorCode.IAM_004);
        }
        if (isOwnDepartmentScopeDenied(query)) {
            return emptyResult(query, sort.normalized());
        }

        log.info("[ACTION] Start ListBudgets | userId={} | page={} | size={}",
                LogMaskingUtil.maskId(query.actorId()),
                query.page(),
                query.size());

        BudgetFilter filter = new BudgetFilter(
                effectiveDepartmentId,
                query.fiscalYear(),
                query.quarter(),
                query.glAccountCode(),
                query.status(),
                query.page(),
                query.size(),
                (query.page() - 1) * query.size(),
                sort.field(),
                sort.direction());

        List<BudgetDashboardView> dashboards = budgetRepository.findByFilter(filter).stream()
                .map(BudgetDashboardView::from)
                .toList();
        long total = budgetRepository.countByFilter(filter);
        log.info("[ACTION] Complete ListBudgets | userId={} | totalCount={}",
                LogMaskingUtil.maskId(query.actorId()),
                total);
        return new PageResult<>(dashboards, PageMeta.of(total, query.page(), query.size(), sort.normalized()));
    }

    private UUID effectiveDepartmentId(ListBudgetsQuery query) {
        if (query.hasPermission(PERMISSION_VIEW_ALL)) {
            return query.departmentId();
        }
        if (query.hasPermission(PERMISSION_VIEW_OWN_DEPT)) {
            return query.actorDepartmentId();
        }
        return null;
    }

    private boolean isOwnDepartmentScopeDenied(ListBudgetsQuery query) {
        return !query.hasPermission(PERMISSION_VIEW_ALL)
                && query.hasPermission(PERMISSION_VIEW_OWN_DEPT)
                && query.departmentId() != null
                && !query.departmentId().equals(query.actorDepartmentId());
    }

    private PageResult<BudgetDashboardView> emptyResult(ListBudgetsQuery query, String sort) {
        return new PageResult<>(List.of(), PageMeta.of(0, query.page(), query.size(), sort));
    }

    private record BudgetSort(String field, String direction) {
        static BudgetSort from(String rawSort) {
            String field = "fiscalYear";
            String direction = "desc";
            if (rawSort != null && !rawSort.isBlank()) {
                String[] parts = rawSort.split(",", 2);
                field = allowedField(parts[0].trim());
                if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                    direction = "asc";
                }
            }
            return new BudgetSort(field, direction);
        }

        String normalized() {
            return field + "," + direction;
        }

        private static String allowedField(String value) {
            return switch (value) {
                case "departmentId", "fiscalYear", "quarter", "glAccountCode", "status",
                        "allocated", "committed", "spent", "available" -> value;
                default -> "fiscalYear";
            };
        }
    }
}
