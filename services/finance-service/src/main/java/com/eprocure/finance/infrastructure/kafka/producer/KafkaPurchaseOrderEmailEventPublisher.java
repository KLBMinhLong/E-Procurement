package com.eprocure.finance.infrastructure.kafka.producer;

import com.eprocure.finance.application.port.out.PurchaseOrderEmailEventPublisher;
import com.eprocure.finance.domain.event.PurchaseOrderEmailRequestedEvent;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "eprocure.finance.integration.kafka-enabled", havingValue = "true")
public class KafkaPurchaseOrderEmailEventPublisher implements PurchaseOrderEmailEventPublisher {
    private static final Logger log = LogManager.getLogger(KafkaPurchaseOrderEmailEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KafkaPurchaseOrderEmailEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${eprocure.finance.kafka.topics.email-send:notification.email.send}") String topic) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(PurchaseOrderEmailRequestedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPurchaseOrderEmailRequested(PurchaseOrderEmailRequestedEvent event) {
        CompletableFuture.runAsync(() -> send(event));
    }

    private void send(PurchaseOrderEmailRequestedEvent event) {
        try {
            kafkaTemplate.send(topic, event.payload().referenceId(), event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.warn("[KAFKA] Publish skipped | topic={} | eventId={} | error={}",
                                    topic,
                                    event.eventId(),
                                    exception.getMessage());
                        } else {
                            log.info("[KAFKA] Published | topic={} | eventId={} | partition={} | offset={}",
                                    topic,
                                    event.eventId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (RuntimeException exception) {
            log.warn("[KAFKA] Publish skipped | topic={} | eventId={} | error={}",
                    topic,
                    event.eventId(),
                    exception.getMessage());
        }
    }
}
