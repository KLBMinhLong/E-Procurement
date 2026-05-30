package com.eprocure.finance.application.port.out;

import com.eprocure.finance.application.service.BudgetDashboardView;
import java.util.Optional;
import java.util.UUID;

public interface BudgetDashboardCachePort {
    Optional<BudgetDashboardView> findByBudgetId(UUID budgetId);

    void store(BudgetDashboardView dashboard);

    void evict(UUID budgetId);
}
