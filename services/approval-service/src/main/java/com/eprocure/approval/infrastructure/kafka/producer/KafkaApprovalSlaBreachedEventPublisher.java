package com.eprocure.approval.infrastructure.kafka.producer;

import com.eprocure.approval.application.port.out.ApprovalSlaBreachedEventPublisher;
import com.eprocure.approval.domain.event.ApprovalSlaBreachedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "eprocure.approval.integration.fallback-enabled", havingValue = "false")
public class KafkaApprovalSlaBreachedEventPublisher implements ApprovalSlaBreachedEventPublisher {
    private static final Logger log = LogManager.getLogger(KafkaApprovalSlaBreachedEventPublisher.class);
    private static final String TOPIC_SLA_BREACHED = "approval.sla.breached";

    private final ApplicationEventPublisher applicationEventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaApprovalSlaBreachedEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(ApprovalSlaBreachedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApprovalSlaBreached(ApprovalSlaBreachedEvent event) {
        String key = event.payload().approvalStepId().toString();
        kafkaTemplate.send(TOPIC_SLA_BREACHED, key, event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("[KAFKA] Publish failed | topic={} | eventId={} | error={}",
                                TOPIC_SLA_BREACHED,
                                event.eventId(),
                                exception.getMessage());
                    } else {
                        log.info("[KAFKA] Published | topic={} | eventId={} | partition={} | offset={}",
                                TOPIC_SLA_BREACHED,
                                event.eventId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
