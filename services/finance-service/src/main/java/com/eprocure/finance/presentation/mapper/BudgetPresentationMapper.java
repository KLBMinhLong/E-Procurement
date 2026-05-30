package com.eprocure.finance.presentation.mapper;

import com.eprocure.finance.application.port.in.ApproveBudgetOverrideCommand;
import com.eprocure.finance.application.port.in.GetBudgetDashboardQuery;
import com.eprocure.finance.application.port.in.ListBudgetsQuery;
import com.eprocure.finance.application.port.in.TransferBudgetCommand;
import com.eprocure.finance.application.service.BudgetDashboardView;
import com.eprocure.finance.application.service.BudgetOverrideApprovalView;
import com.eprocure.finance.application.service.BudgetTransferView;
import com.eprocure.finance.common.security.UserPrincipal;
import com.eprocure.finance.domain.model.BudgetStatus;
import com.eprocure.finance.domain.model.vo.Money;
import com.eprocure.finance.presentation.request.ApproveBudgetOverrideRequest;
import com.eprocure.finance.presentation.request.TransferBudgetRequest;
import com.eprocure.finance.presentation.response.BudgetDashboardResponse;
import com.eprocure.finance.presentation.response.BudgetOverrideApprovalResponse;
import com.eprocure.finance.presentation.response.BudgetTransferResponse;
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

    public ApproveBudgetOverrideCommand toOverrideCommand(
            UserPrincipal principal,
            UUID budgetId,
            ApproveBudgetOverrideRequest request) {
        return new ApproveBudgetOverrideCommand(
                principal.getId(),
                budgetId,
                request.prId(),
                new Money(request.overrideAmount(), currencyOrDefault(request.currency())),
                request.overrideReason());
    }

    public TransferBudgetCommand toTransferCommand(
            UserPrincipal principal,
            UUID sourceBudgetId,
            TransferBudgetRequest request) {
        return new TransferBudgetCommand(
                principal.getId(),
                sourceBudgetId,
                request.targetBudgetId(),
                new Money(request.amount(), currencyOrDefault(request.currency())),
                request.reason());
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

    public BudgetOverrideApprovalResponse toResponse(BudgetOverrideApprovalView view) {
        return new BudgetOverrideApprovalResponse(
                view.id(),
                view.budgetId(),
                view.purchaseRequestId(),
                format(view.overrideAmount()),
                view.overrideAmount().currency(),
                view.overrideReason(),
                view.approvedBy(),
                view.approvedAt(),
                view.status());
    }

    public BudgetTransferResponse toResponse(BudgetTransferView view) {
        return new BudgetTransferResponse(
                view.id(),
                view.sourceBudgetId(),
                view.targetBudgetId(),
                format(view.amount()),
                view.amount().currency(),
                view.reason(),
                view.approvedBy(),
                view.approvedAt(),
                toResponse(view.sourceDashboard()),
                toResponse(view.targetDashboard()));
    }

    private String format(Money money) {
        return money.amount().toPlainString();
    }

    private String currencyOrDefault(String currency) {
        return currency == null || currency.isBlank() ? "VND" : currency;
    }
}
