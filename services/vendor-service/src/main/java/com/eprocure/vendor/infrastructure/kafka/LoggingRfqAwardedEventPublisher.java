package com.eprocure.vendor.infrastructure.kafka;

import com.eprocure.vendor.application.port.out.RfqAwardedEventPublisher;
import com.eprocure.vendor.common.util.LogMaskingUtil;
import com.eprocure.vendor.domain.event.RfqAwardedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "eprocure.vendor.integration.kafka-enabled", havingValue = "false", matchIfMissing = true)
public class LoggingRfqAwardedEventPublisher implements RfqAwardedEventPublisher {
    private static final Logger log = LogManager.getLogger(LoggingRfqAwardedEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public LoggingRfqAwardedEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(RfqAwardedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRfqAwarded(RfqAwardedEvent event) {
        log.info("[AUDIT] rfq awarded event | eventId={} | rfqId={} | prId={} | vendorId={} | quoteId={} | amount={} {}",
                event.eventId(),
                LogMaskingUtil.maskId(event.payload().rfqId()),
                LogMaskingUtil.maskId(event.payload().prId()),
                LogMaskingUtil.maskId(event.payload().vendorId()),
                LogMaskingUtil.maskId(event.payload().awardedQuoteId()),
                event.payload().totalAmount(),
                event.payload().currency());
    }
}
