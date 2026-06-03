package com.eprocure.finance.infrastructure.kafka;

import com.eprocure.finance.application.port.out.PurchaseOrderIssuedEventPublisher;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.event.PurchaseOrderIssuedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.finance.integration.kafka-enabled", havingValue = "false", matchIfMissing = true)
public class LoggingPurchaseOrderIssuedEventPublisher implements PurchaseOrderIssuedEventPublisher {
    private static final Logger log = LogManager.getLogger(LoggingPurchaseOrderIssuedEventPublisher.class);

    @Override
    public void publish(PurchaseOrderIssuedEvent event) {
        log.info("[AUDIT] purchase order issued event | eventId={} | poId={} | vendorId={}",
                event.eventId(),
                LogMaskingUtil.maskId(event.payload().poId()),
                LogMaskingUtil.maskId(event.payload().vendorId()));
    }
}
