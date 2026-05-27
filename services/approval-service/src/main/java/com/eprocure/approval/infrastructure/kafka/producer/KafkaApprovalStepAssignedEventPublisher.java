package com.eprocure.approval.infrastructure.kafka.producer;

import com.eprocure.approval.application.port.out.ApprovalStepAssignedEventPublisher;
import com.eprocure.approval.domain.event.ApprovalStepAssignedEvent;
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
public class KafkaApprovalStepAssignedEventPublisher implements ApprovalStepAssignedEventPublisher {
    private static final Logger log = LogManager.getLogger(KafkaApprovalStepAssignedEventPublisher.class);
    private static final String TOPIC_STEP_ASSIGNED = "approval.step.assigned";

    private final ApplicationEventPublisher applicationEventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaApprovalStepAssignedEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(ApprovalStepAssignedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApprovalStepAssigned(ApprovalStepAssignedEvent event) {
        String key = event.payload().approvalStepId().toString();
        kafkaTemplate.send(TOPIC_STEP_ASSIGNED, key, event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("[KAFKA] Publish failed | topic={} | eventId={} | error={}",
                                TOPIC_STEP_ASSIGNED,
                                event.eventId(),
                                exception.getMessage());
                    } else {
                        log.info("[KAFKA] Published | topic={} | eventId={} | partition={} | offset={}",
                                TOPIC_STEP_ASSIGNED,
                                event.eventId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
