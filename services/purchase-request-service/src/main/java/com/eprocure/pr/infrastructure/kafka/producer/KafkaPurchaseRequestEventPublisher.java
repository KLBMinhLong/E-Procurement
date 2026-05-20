package com.eprocure.pr.infrastructure.kafka.producer;

import com.eprocure.pr.application.port.out.PrCancelledEventPublisher;
import com.eprocure.pr.application.port.out.PrSubmittedEventPublisher;
import com.eprocure.pr.domain.event.PrCancelledEvent;
import com.eprocure.pr.domain.event.PrSubmittedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "eprocure.pr.integration.fallback-enabled", havingValue = "false", matchIfMissing = false)
public class KafkaPurchaseRequestEventPublisher implements PrSubmittedEventPublisher, PrCancelledEventPublisher {

    private static final Logger log = LogManager.getLogger(KafkaPurchaseRequestEventPublisher.class);
    
    private static final String TOPIC_SUBMITTED = "procurement.pr.submitted";
    private static final String TOPIC_CANCELLED = "procurement.pr.cancelled";

    private final ApplicationEventPublisher applicationEventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaPurchaseRequestEventPublisher(ApplicationEventPublisher applicationEventPublisher, KafkaTemplate<String, Object> kafkaTemplate) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.kafkaTemplate = kafkaTemplate;
    }

    // --- Port Interface Implementation ---
    // Instead of sending to Kafka immediately (which might happen before DB commit),
    // we publish it as an internal Spring ApplicationEvent.

    @Override
    public void publish(PrSubmittedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publish(PrCancelledEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    // --- Transactional Event Listeners ---
    // These will be triggered automatically AFTER the current database transaction commits.

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPrSubmittedEvent(PrSubmittedEvent event) {
        String key = event.payload().purchaseRequestId().toString();
        
        kafkaTemplate.send(TOPIC_SUBMITTED, key, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[KAFKA] Publish failed | topic={} | eventId={} | error={}",
                            TOPIC_SUBMITTED, event.eventId(), ex.getMessage());
                } else {
                    log.info("[KAFKA] Published | topic={} | eventId={} | partition={} | offset={}",
                            TOPIC_SUBMITTED, event.eventId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPrCancelledEvent(PrCancelledEvent event) {
        String key = event.payload().purchaseRequestId().toString();

        kafkaTemplate.send(TOPIC_CANCELLED, key, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[KAFKA] Publish failed | topic={} | eventId={} | error={}",
                            TOPIC_CANCELLED, event.eventId(), ex.getMessage());
                } else {
                    log.info("[KAFKA] Published | topic={} | eventId={} | partition={} | offset={}",
                            TOPIC_CANCELLED, event.eventId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
    }
}
