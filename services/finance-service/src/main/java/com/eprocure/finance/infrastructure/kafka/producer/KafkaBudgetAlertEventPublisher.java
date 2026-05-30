package com.eprocure.finance.infrastructure.kafka.producer;

import com.eprocure.finance.application.port.out.BudgetAlertEventPublisher;
import com.eprocure.finance.domain.event.BudgetAlertEvent;
import com.eprocure.finance.domain.model.BudgetAlertType;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "eprocure.finance.integration.kafka-enabled", havingValue = "true")
public class KafkaBudgetAlertEventPublisher implements BudgetAlertEventPublisher {
    private static final Logger log = LogManager.getLogger(KafkaBudgetAlertEventPublisher.class);
    private static final String TOPIC_WARNING = "finance.budget.warning";
    private static final String TOPIC_EXCEEDED = "finance.budget.exceeded";

    private final ApplicationEventPublisher applicationEventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaBudgetAlertEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(BudgetAlertEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBudgetAlert(BudgetAlertEvent event) {
        CompletableFuture.runAsync(() -> send(event));
    }

    private void send(BudgetAlertEvent event) {
        String topic = topicFor(event.payload().alertType());
        String key = event.payload().budgetId().toString();
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

    private String topicFor(BudgetAlertType alertType) {
        return alertType == BudgetAlertType.EXCEEDED ? TOPIC_EXCEEDED : TOPIC_WARNING;
    }
}
