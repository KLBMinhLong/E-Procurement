package com.eprocure.inventory.infrastructure.kafka.producer;

import com.eprocure.inventory.application.port.out.GrCreatedEventPublisher;
import com.eprocure.inventory.domain.event.GrCreatedEvent;
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
@ConditionalOnProperty(name = "eprocure.inventory.integration.kafka-enabled", havingValue = "true")
public class KafkaGrCreatedEventPublisher implements GrCreatedEventPublisher {
    private static final Logger log = LogManager.getLogger(KafkaGrCreatedEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KafkaGrCreatedEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${eprocure.inventory.kafka.topics.gr-created:inventory.gr.created}") String topic) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(GrCreatedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGrCreated(GrCreatedEvent event) {
        CompletableFuture.runAsync(() -> send(event));
    }

    private void send(GrCreatedEvent event) {
        try {
            kafkaTemplate.send(topic, event.payload().grId().toString(), event)
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
