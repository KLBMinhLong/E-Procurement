package com.eprocure.notification.infrastructure.kafka.consumer;

import com.eprocure.notification.application.port.in.BusinessEventCommand;
import com.eprocure.notification.application.usecase.ConsumeBusinessEventUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.notification.integration.kafka-enabled", havingValue = "true")
public class BusinessEventNotificationConsumer {
    private static final Logger log = LogManager.getLogger(BusinessEventNotificationConsumer.class);

    private final ConsumeBusinessEventUseCase consumeBusinessEventUseCase;
    private final ObjectMapper objectMapper;

    public BusinessEventNotificationConsumer(
            ConsumeBusinessEventUseCase consumeBusinessEventUseCase,
            ObjectMapper objectMapper) {
        this.consumeBusinessEventUseCase = consumeBusinessEventUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {
            "${eprocure.notification.kafka.topics.approval-step-assigned:approval.step.assigned}",
            "${eprocure.notification.kafka.topics.approval-sla-warning:approval.sla.warning}",
            "${eprocure.notification.kafka.topics.approval-sla-breached:approval.sla.breached}",
            "${eprocure.notification.kafka.topics.pr-approved:procurement.pr.approved}",
            "${eprocure.notification.kafka.topics.pr-rejected:procurement.pr.rejected}",
            "${eprocure.notification.kafka.topics.pr-changes-requested:procurement.pr.changes-requested}",
            "${eprocure.notification.kafka.topics.budget-warning:finance.budget.warning}",
            "${eprocure.notification.kafka.topics.budget-exceeded:finance.budget.exceeded}",
            "${eprocure.notification.kafka.topics.email-send:notification.email.send}"
    })
    public void consume(ConsumerRecord<String, String> record) {
        Optional<BusinessEventCommand> parsed = toCommand(record);
        if (parsed.isEmpty()) {
            return;
        }
        BusinessEventCommand command = parsed.get();
        int created = consumeBusinessEventUseCase.execute(command);
        log.info("[KAFKA] Consumed business event for notification | topic={} | eventId={} | created={}",
                record.topic(),
                command.eventId(),
                created);
    }

    Optional<BusinessEventCommand> toCommand(ConsumerRecord<String, String> record) {
        try {
            JsonNode root = objectMapper.readTree(record.value());
            JsonNode payload = root.path("payload");
            if (payload.isMissingNode() || payload.isNull()) {
                throw new IllegalArgumentException("Kafka event payload is required");
            }
            return Optional.of(new BusinessEventCommand(
                    requiredText(root, "eventId"),
                    requiredText(root, "eventType"),
                    requiredText(root, "source"),
                    requiredTimestamp(root),
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    payload));
        } catch (Exception exception) {
            log.warn("[KAFKA] Skip invalid notification event | topic={} | reason={}",
                    record.topic(),
                    exception.getMessage());
            return Optional.empty();
        }
    }

    private Instant requiredTimestamp(JsonNode root) {
        JsonNode value = root.get("timestamp");
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("timestamp is required");
        }
        if (value.isTextual() && !value.asText().isBlank()) {
            return Instant.parse(value.asText());
        }
        if (value.isNumber()) {
            return parseNumericEpochSeconds(value.decimalValue());
        }
        throw new IllegalArgumentException("timestamp is required");
    }

    private Instant parseNumericEpochSeconds(BigDecimal value) {
        BigDecimal secondsPart = value.setScale(0, RoundingMode.DOWN);
        BigDecimal nanosPart = value.subtract(secondsPart)
                .movePointRight(9)
                .setScale(0, RoundingMode.DOWN);
        return Instant.ofEpochSecond(secondsPart.longValue(), nanosPart.longValue());
    }

    private String requiredText(JsonNode root, String fieldName) {
        JsonNode value = root.get(fieldName);
        if (value == null || value.isNull() || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.asText();
    }
}
