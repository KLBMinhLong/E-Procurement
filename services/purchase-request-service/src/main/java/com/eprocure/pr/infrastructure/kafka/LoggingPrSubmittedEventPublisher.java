package com.eprocure.pr.infrastructure.kafka;

import com.eprocure.pr.application.port.out.PrSubmittedEventPublisher;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.domain.event.PrSubmittedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.pr.integration.fallback-enabled", havingValue = "true", matchIfMissing = true)
public class LoggingPrSubmittedEventPublisher implements PrSubmittedEventPublisher {
    private static final Logger log = LogManager.getLogger(LoggingPrSubmittedEventPublisher.class);

    @Override
    public void publish(PrSubmittedEvent event) {
        log.info("[AUDIT] pr submitted event | eventId={} | prId={} | prNumber={}",
                event.eventId(),
                LogMaskingUtil.maskId(event.payload().purchaseRequestId()),
                event.payload().prNumber());
    }
}
