package com.eprocure.finance.application.port.out;

import com.eprocure.finance.domain.event.BudgetAlertEvent;

public interface BudgetAlertEventPublisher {
    void publish(BudgetAlertEvent event);
}
