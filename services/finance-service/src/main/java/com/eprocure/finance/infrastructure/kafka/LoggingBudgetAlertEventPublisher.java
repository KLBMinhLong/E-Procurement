package com.eprocure.finance.infrastructure.kafka;

import com.eprocure.finance.application.port.out.BudgetAlertEventPublisher;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.event.BudgetAlertEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.finance.integration.kafka-enabled", havingValue = "false", matchIfMissing = true)
public class LoggingBudgetAlertEventPublisher implements BudgetAlertEventPublisher {
    private static final Logger log = LogManager.getLogger(LoggingBudgetAlertEventPublisher.class);

    @Override
    public void publish(BudgetAlertEvent event) {
        log.info("[AUDIT] finance budget alert event | eventId={} | type={} | budgetId={} | departmentId={}",
                event.eventId(),
                event.payload().alertType(),
                LogMaskingUtil.maskId(event.payload().budgetId()),
                LogMaskingUtil.maskId(event.payload().departmentId()));
    }
}
