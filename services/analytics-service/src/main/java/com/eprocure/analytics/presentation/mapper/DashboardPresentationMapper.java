package com.eprocure.analytics.presentation.mapper;

import com.eprocure.analytics.application.port.in.GetExecutiveDashboardQuery;
import com.eprocure.analytics.application.port.in.GetRoleDashboardQuery;
import com.eprocure.analytics.common.security.UserPrincipal;
import com.eprocure.analytics.domain.model.ChartDataPoint;
import com.eprocure.analytics.domain.model.DepartmentSpend;
import com.eprocure.analytics.domain.model.ExecutiveDashboard;
import com.eprocure.analytics.domain.model.KpiCard;
import com.eprocure.analytics.domain.model.MonthlyTrend;
import com.eprocure.analytics.domain.model.TopVendor;
import com.eprocure.analytics.domain.model.Trend;
import com.eprocure.analytics.domain.model.dashboard.BudgetStatus;
import com.eprocure.analytics.domain.model.dashboard.DepartmentBudgetSummary;
import com.eprocure.analytics.domain.model.dashboard.ManagerDashboard;
import com.eprocure.analytics.domain.model.dashboard.MyPurchaseRequestStats;
import com.eprocure.analytics.domain.model.dashboard.PendingApprovals;
import com.eprocure.analytics.domain.model.dashboard.PoPipeline;
import com.eprocure.analytics.domain.model.dashboard.PurchasingDashboard;
import com.eprocure.analytics.domain.model.dashboard.RecentPurchaseRequest;
import com.eprocure.analytics.domain.model.dashboard.RequesterDashboard;
import com.eprocure.analytics.domain.model.dashboard.SlaWarning;
import com.eprocure.analytics.domain.model.dashboard.VendorPerformance;
import com.eprocure.analytics.presentation.response.ApprovalSlaResponse;
import com.eprocure.analytics.presentation.response.BudgetStatusResponse;
import com.eprocure.analytics.presentation.response.ChartDataPointResponse;
import com.eprocure.analytics.presentation.response.DepartmentSpendResponse;
import com.eprocure.analytics.presentation.response.DepartmentBudgetSummaryResponse;
import com.eprocure.analytics.presentation.response.ExecutiveDashboardResponse;
import com.eprocure.analytics.presentation.response.KpiCardResponse;
import com.eprocure.analytics.presentation.response.ManagerDashboardResponse;
import com.eprocure.analytics.presentation.response.ManagerRecentPrResponse;
import com.eprocure.analytics.presentation.response.MonthlyTrendResponse;
import com.eprocure.analytics.presentation.response.MyPurchaseRequestStatsResponse;
import com.eprocure.analytics.presentation.response.PendingApprovalsResponse;
import com.eprocure.analytics.presentation.response.PoPipelineResponse;
import com.eprocure.analytics.presentation.response.PurchasingDashboardResponse;
import com.eprocure.analytics.presentation.response.RequesterDashboardResponse;
import com.eprocure.analytics.presentation.response.RequesterRecentPrResponse;
import com.eprocure.analytics.presentation.response.SlaWarningResponse;
import com.eprocure.analytics.presentation.response.TopVendorResponse;
import com.eprocure.analytics.presentation.response.TrendResponse;
import com.eprocure.analytics.presentation.response.VendorPerformanceResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Year;
import org.springframework.stereotype.Component;

@Component
public class DashboardPresentationMapper {
    private final Clock clock;

    public DashboardPresentationMapper(Clock clock) {
        this.clock = clock;
    }

    public GetExecutiveDashboardQuery toQuery(UserPrincipal principal, Integer fiscalYear, Integer quarter) {
        int resolvedFiscalYear = fiscalYear == null ? Year.now(clock).getValue() : fiscalYear;
        return new GetExecutiveDashboardQuery(principal.getId(), resolvedFiscalYear, quarter);
    }

    public GetRoleDashboardQuery toRoleQuery(UserPrincipal principal) {
        return new GetRoleDashboardQuery(principal.getId(), principal.getDepartmentId());
    }

    public ExecutiveDashboardResponse toResponse(ExecutiveDashboard dashboard) {
        return new ExecutiveDashboardResponse(
                dashboard.kpis().stream().map(this::toResponse).toList(),
                dashboard.spendByDepartment().stream().map(this::toResponse).toList(),
                dashboard.spendByCategory().stream().map(this::toResponse).toList(),
                dashboard.monthlyTrend().stream().map(this::toResponse).toList(),
                new ApprovalSlaResponse(
                        dashboard.approvalSla().onTimePercent(),
                        dashboard.approvalSla().avgCycleHours(),
                        dashboard.approvalSla().overdueCount()),
                dashboard.topVendors().stream().map(this::toResponse).toList(),
                dashboard.cachedAt());
    }

    public ManagerDashboardResponse toResponse(ManagerDashboard dashboard) {
        return new ManagerDashboardResponse(
                dashboard.kpis().stream().map(this::toResponse).toList(),
                toResponse(dashboard.budgetStatus()),
                toResponse(dashboard.pendingApprovals()),
                dashboard.recentPrs().stream().map(this::toManagerRecentPrResponse).toList(),
                dashboard.slaWarnings().stream().map(this::toResponse).toList());
    }

    public PurchasingDashboardResponse toResponse(PurchasingDashboard dashboard) {
        return new PurchasingDashboardResponse(
                dashboard.kpis().stream().map(this::toResponse).toList(),
                toResponse(dashboard.poPipeline()),
                dashboard.openRfqs(),
                dashboard.grPending(),
                dashboard.invoicesPendingMatch(),
                dashboard.vendorPerformance().stream().map(this::toResponse).toList());
    }

    public RequesterDashboardResponse toResponse(RequesterDashboard dashboard) {
        return new RequesterDashboardResponse(
                toResponse(dashboard.myPrStats()),
                toResponse(dashboard.departmentBudget()),
                dashboard.recentPrs().stream().map(this::toRequesterRecentPrResponse).toList());
    }

    private KpiCardResponse toResponse(KpiCard card) {
        return new KpiCardResponse(
                card.label(),
                card.value(),
                card.unit(),
                toResponse(card.trend()),
                card.status());
    }

    private TrendResponse toResponse(Trend trend) {
        if (trend == null) {
            return null;
        }
        return new TrendResponse(trend.direction(), trend.percent(), trend.vsLabel());
    }

    private DepartmentSpendResponse toResponse(DepartmentSpend item) {
        return new DepartmentSpendResponse(
                item.departmentCode(),
                item.departmentName(),
                money(item.spent()),
                money(item.budget()),
                item.utilization(),
                item.status());
    }

    private ChartDataPointResponse toResponse(ChartDataPoint point) {
        return new ChartDataPointResponse(point.label(), point.value(), point.value2());
    }

    private MonthlyTrendResponse toResponse(MonthlyTrend point) {
        return new MonthlyTrendResponse(point.month(), point.spent(), point.budget(), point.prCount());
    }

    private TopVendorResponse toResponse(TopVendor vendor) {
        return new TopVendorResponse(
                vendor.vendorName(),
                money(vendor.totalSpent()),
                vendor.orderCount(),
                vendor.avgScore());
    }

    private BudgetStatusResponse toResponse(BudgetStatus budgetStatus) {
        return new BudgetStatusResponse(
                money(budgetStatus.allocated()),
                money(budgetStatus.committed()),
                money(budgetStatus.spent()),
                money(budgetStatus.available()),
                budgetStatus.availablePct(),
                budgetStatus.forecastRunOutDate());
    }

    private PendingApprovalsResponse toResponse(PendingApprovals pendingApprovals) {
        return new PendingApprovalsResponse(
                pendingApprovals.count(),
                pendingApprovals.overdueCount(),
                pendingApprovals.urgentCount());
    }

    private ManagerRecentPrResponse toManagerRecentPrResponse(RecentPurchaseRequest pr) {
        return new ManagerRecentPrResponse(
                pr.prNumber(),
                pr.title(),
                pr.status(),
                money(pr.totalAmount()),
                pr.requester(),
                pr.createdAt());
    }

    private SlaWarningResponse toResponse(SlaWarning warning) {
        return new SlaWarningResponse(
                warning.taskId(),
                warning.prNumber(),
                warning.slaDeadline(),
                warning.overdue());
    }

    private PoPipelineResponse toResponse(PoPipeline pipeline) {
        return new PoPipelineResponse(
                pipeline.draft(),
                pipeline.pendingApproval(),
                pipeline.sentToVendor(),
                pipeline.partiallyReceived());
    }

    private VendorPerformanceResponse toResponse(VendorPerformance vendorPerformance) {
        return new VendorPerformanceResponse(
                vendorPerformance.vendorName(),
                vendorPerformance.onTimeDelivery(),
                vendorPerformance.qualityScore(),
                vendorPerformance.pendingOrders());
    }

    private MyPurchaseRequestStatsResponse toResponse(MyPurchaseRequestStats stats) {
        return new MyPurchaseRequestStatsResponse(
                stats.draft(),
                stats.pendingApproval(),
                stats.changesRequested(),
                stats.approved(),
                stats.rejected());
    }

    private DepartmentBudgetSummaryResponse toResponse(DepartmentBudgetSummary budget) {
        return new DepartmentBudgetSummaryResponse(money(budget.available()), budget.availablePct());
    }

    private RequesterRecentPrResponse toRequesterRecentPrResponse(RecentPurchaseRequest pr) {
        return new RequesterRecentPrResponse(
                pr.prNumber(),
                pr.title(),
                pr.status(),
                pr.currentApprover(),
                money(pr.totalAmount()));
    }

    private String money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).toPlainString();
    }
}
