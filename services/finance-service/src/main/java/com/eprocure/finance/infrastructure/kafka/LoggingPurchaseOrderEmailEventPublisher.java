package com.eprocure.finance.infrastructure.kafka;

import com.eprocure.finance.application.port.out.PurchaseOrderEmailEventPublisher;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.event.PurchaseOrderEmailRequestedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.finance.integration.kafka-enabled", havingValue = "false", matchIfMissing = true)
public class LoggingPurchaseOrderEmailEventPublisher implements PurchaseOrderEmailEventPublisher {
    private static final Logger log = LogManager.getLogger(LoggingPurchaseOrderEmailEventPublisher.class);

    @Override
    public void publish(PurchaseOrderEmailRequestedEvent event) {
        log.info("[AUDIT] purchase order email event | eventId={} | poNumber={} | to={}",
                event.eventId(),
                event.payload().poNumber(),
                LogMaskingUtil.maskEmail(event.payload().recipientEmail()));
    }
}
