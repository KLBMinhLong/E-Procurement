package com.eprocure.vendor.infrastructure.kafka.producer;

import com.eprocure.vendor.application.port.out.RfqAwardedEventPublisher;
import com.eprocure.vendor.domain.event.RfqAwardedEvent;
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
@ConditionalOnProperty(name = "eprocure.vendor.integration.kafka-enabled", havingValue = "true")
public class KafkaRfqAwardedEventPublisher implements RfqAwardedEventPublisher {
    private static final Logger log = LogManager.getLogger(KafkaRfqAwardedEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KafkaRfqAwardedEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${eprocure.vendor.kafka.topics.rfq-awarded:procurement.rfq.awarded}") String topic) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(RfqAwardedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRfqAwarded(RfqAwardedEvent event) {
        CompletableFuture.runAsync(() -> send(event));
    }

    private void send(RfqAwardedEvent event) {
        String key = event.payload().rfqId().toString();
        try {
            kafkaTemplate.send(topic, key, event)
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
