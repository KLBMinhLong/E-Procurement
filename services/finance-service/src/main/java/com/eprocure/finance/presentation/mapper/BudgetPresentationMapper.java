package com.eprocure.finance.presentation.mapper;

import com.eprocure.finance.application.port.in.GetBudgetDashboardQuery;
import com.eprocure.finance.application.port.in.ListBudgetsQuery;
import com.eprocure.finance.application.service.BudgetDashboardView;
import com.eprocure.finance.common.security.UserPrincipal;
import com.eprocure.finance.domain.model.BudgetStatus;
import com.eprocure.finance.domain.model.vo.Money;
import com.eprocure.finance.presentation.response.BudgetDashboardResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BudgetPresentationMapper {

    public ListBudgetsQuery toListQuery(
            UserPrincipal principal,
            UUID departmentId,
            Integer fiscalYear,
            Integer quarter,
            String glAccountCode,
            BudgetStatus status,
            int page,
            int size,
            String sort) {
        return new ListBudgetsQuery(
                principal.getId(),
                principal.getDepartmentId(),
                principal.getPermissions(),
                departmentId,
                fiscalYear,
                quarter,
                glAccountCode,
                status,
                page,
                size,
                sort);
    }

    public GetBudgetDashboardQuery toDashboardQuery(UserPrincipal principal, UUID budgetId) {
        return new GetBudgetDashboardQuery(
                principal.getId(),
                principal.getDepartmentId(),
                principal.getPermissions(),
                budgetId);
    }

    public BudgetDashboardResponse toResponse(BudgetDashboardView view) {
        return new BudgetDashboardResponse(
                view.id(),
                view.departmentId(),
                view.fiscalYear(),
                view.quarter(),
                view.glAccountCode(),
                format(view.allocated()),
                format(view.committed()),
                format(view.spent()),
                format(view.available()),
                view.availablePercent(),
                view.burnRatePerMonth() == null ? null : format(view.burnRatePerMonth()),
                view.forecastExhaustedAt(),
                view.status());
    }

    private String format(Money money) {
        return money.amount().toPlainString();
    }
}
