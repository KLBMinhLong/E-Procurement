package com.eprocure.approval.infrastructure.kafka.consumer;

import com.eprocure.approval.application.port.in.StartApprovalProcessCommand;
import com.eprocure.approval.application.usecase.StartApprovalProcessUseCase;
import com.eprocure.approval.common.util.LogMaskingUtil;
import com.eprocure.approval.domain.model.PurchaseRequestPriority;
import com.eprocure.approval.domain.model.vo.Money;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.approval.integration.fallback-enabled", havingValue = "false")
public class PrSubmittedEventConsumer {
    private static final Logger log = LogManager.getLogger(PrSubmittedEventConsumer.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final StartApprovalProcessUseCase startApprovalProcessUseCase;
    private final ObjectMapper objectMapper;

    public PrSubmittedEventConsumer(
            StartApprovalProcessUseCase startApprovalProcessUseCase,
            ObjectMapper objectMapper) {
        this.startApprovalProcessUseCase = startApprovalProcessUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${eprocure.approval.kafka.topics.pr-submitted:procurement.pr.submitted}")
    public void consume(ConsumerRecord<String, String> record) {
        PrSubmittedKafkaEvent event = parse(record.value());
        log.info("[KAFKA] Consumed PR submitted | topic={} | partition={} | offset={} | eventId={} | prId={}",
                record.topic(),
                record.partition(),
                record.offset(),
                event.eventId(),
                LogMaskingUtil.maskId(event.payload().purchaseRequestId()));

        startApprovalProcessUseCase.execute(new StartApprovalProcessCommand(
                event.eventId(),
                record.topic(),
                record.partition(),
                record.offset(),
                event.traceId(),
                event.payload().purchaseRequestId(),
                event.payload().prNumber(),
                event.payload().title(),
                event.payload().requesterId(),
                event.payload().departmentId(),
                new Money(event.payload().totalAmount().amount(), event.payload().totalAmount().currency()),
                event.payload().categories(),
                PurchaseRequestPriority.valueOf(event.payload().priority()),
                objectMapper.convertValue(event.payload(), MAP_TYPE)));
    }

    private PrSubmittedKafkaEvent parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PR submitted event payload must not be blank");
        }
        try {
            return objectMapper.readValue(value, PrSubmittedKafkaEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Cannot parse PR submitted event payload", exception);
        }
    }

    record PrSubmittedKafkaEvent(
            String eventId,
            String eventType,
            String version,
            String source,
            Instant timestamp,
            UUID traceId,
            Payload payload) {
    }

    record Payload(
            UUID purchaseRequestId,
            String prNumber,
            String title,
            UUID requesterId,
            UUID departmentId,
            String priority,
            MoneyPayload totalAmount,
            Set<String> categories) {
    }

    record MoneyPayload(BigDecimal amount, String currency) {
    }
}
