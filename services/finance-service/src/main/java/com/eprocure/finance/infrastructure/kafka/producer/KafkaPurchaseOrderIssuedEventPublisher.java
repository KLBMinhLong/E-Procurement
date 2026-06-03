package com.eprocure.finance.infrastructure.kafka.producer;

import com.eprocure.finance.application.port.out.PurchaseOrderIssuedEventPublisher;
import com.eprocure.finance.domain.event.PurchaseOrderIssuedEvent;
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
public class KafkaPurchaseOrderIssuedEventPublisher implements PurchaseOrderIssuedEventPublisher {
    private static final Logger log = LogManager.getLogger(KafkaPurchaseOrderIssuedEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KafkaPurchaseOrderIssuedEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${eprocure.finance.kafka.topics.po-issued:procurement.po.issued}") String topic) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(PurchaseOrderIssuedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPurchaseOrderIssued(PurchaseOrderIssuedEvent event) {
        CompletableFuture.runAsync(() -> send(event));
    }

    private void send(PurchaseOrderIssuedEvent event) {
        try {
            kafkaTemplate.send(topic, event.payload().poId().toString(), event)
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
