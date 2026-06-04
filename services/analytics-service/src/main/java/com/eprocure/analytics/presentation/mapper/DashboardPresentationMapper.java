package com.eprocure.analytics.presentation.mapper;

import com.eprocure.analytics.application.port.in.GetExecutiveDashboardQuery;
import com.eprocure.analytics.common.security.UserPrincipal;
import com.eprocure.analytics.domain.model.ChartDataPoint;
import com.eprocure.analytics.domain.model.DepartmentSpend;
import com.eprocure.analytics.domain.model.ExecutiveDashboard;
import com.eprocure.analytics.domain.model.KpiCard;
import com.eprocure.analytics.domain.model.MonthlyTrend;
import com.eprocure.analytics.domain.model.TopVendor;
import com.eprocure.analytics.domain.model.Trend;
import com.eprocure.analytics.presentation.response.ApprovalSlaResponse;
import com.eprocure.analytics.presentation.response.ChartDataPointResponse;
import com.eprocure.analytics.presentation.response.DepartmentSpendResponse;
import com.eprocure.analytics.presentation.response.ExecutiveDashboardResponse;
import com.eprocure.analytics.presentation.response.KpiCardResponse;
import com.eprocure.analytics.presentation.response.MonthlyTrendResponse;
import com.eprocure.analytics.presentation.response.TopVendorResponse;
import com.eprocure.analytics.presentation.response.TrendResponse;
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

    private String money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).toPlainString();
    }
}
