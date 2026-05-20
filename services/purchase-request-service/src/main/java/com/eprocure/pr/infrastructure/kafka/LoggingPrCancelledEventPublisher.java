package com.eprocure.pr.infrastructure.kafka;

import com.eprocure.pr.application.port.out.PrCancelledEventPublisher;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.domain.event.PrCancelledEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.pr.integration.fallback-enabled", havingValue = "true", matchIfMissing = true)
public class LoggingPrCancelledEventPublisher implements PrCancelledEventPublisher {
    private static final Logger log = LogManager.getLogger(LoggingPrCancelledEventPublisher.class);

    @Override
    public void publish(PrCancelledEvent event) {
        log.info("[AUDIT] pr cancelled event | eventId={} | prId={} | prNumber={}",
                event.eventId(),
                LogMaskingUtil.maskId(event.payload().purchaseRequestId()),
                event.payload().prNumber());
    }
}
