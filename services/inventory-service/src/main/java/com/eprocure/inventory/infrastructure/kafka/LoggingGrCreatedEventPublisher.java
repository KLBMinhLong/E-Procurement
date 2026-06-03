package com.eprocure.inventory.infrastructure.kafka;

import com.eprocure.inventory.application.port.out.GrCreatedEventPublisher;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.eprocure.inventory.domain.event.GrCreatedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "eprocure.inventory.integration.kafka-enabled", havingValue = "false", matchIfMissing = true)
public class LoggingGrCreatedEventPublisher implements GrCreatedEventPublisher {
    private static final Logger log = LogManager.getLogger(LoggingGrCreatedEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public LoggingGrCreatedEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(GrCreatedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGrCreated(GrCreatedEvent event) {
        log.info("[AUDIT] goods receipt created event | eventId={} | grId={} | poId={} | status={}",
                event.eventId(),
                LogMaskingUtil.maskId(event.payload().grId()),
                LogMaskingUtil.maskId(event.payload().poId()),
                event.payload().status());
    }
}
