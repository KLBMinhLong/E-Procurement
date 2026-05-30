package com.eprocure.finance.infrastructure.kafka.consumer;

import com.eprocure.finance.application.port.in.RecordBudgetCommitmentCommand;
import com.eprocure.finance.application.port.in.ReleaseBudgetCommitmentCommand;
import com.eprocure.finance.application.usecase.FirmCommitBudgetUseCase;
import com.eprocure.finance.application.usecase.ReleaseBudgetCommitmentUseCase;
import com.eprocure.finance.application.usecase.TentativeCommitBudgetUseCase;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.vo.Money;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.finance.integration.kafka-enabled", havingValue = "true")
public class PurchaseRequestBudgetEventConsumer {
    private static final Logger log = LogManager.getLogger(PurchaseRequestBudgetEventConsumer.class);

    private final TentativeCommitBudgetUseCase tentativeCommitBudgetUseCase;
    private final FirmCommitBudgetUseCase firmCommitBudgetUseCase;
    private final ReleaseBudgetCommitmentUseCase releaseBudgetCommitmentUseCase;
    private final ObjectMapper objectMapper;
    private final String defaultGlAccountCode;

    public PurchaseRequestBudgetEventConsumer(
            TentativeCommitBudgetUseCase tentativeCommitBudgetUseCase,
            FirmCommitBudgetUseCase firmCommitBudgetUseCase,
            ReleaseBudgetCommitmentUseCase releaseBudgetCommitmentUseCase,
            ObjectMapper objectMapper,
            @Value("${eprocure.finance.integration.default-gl-account-code:6002}") String defaultGlAccountCode) {
        this.tentativeCommitBudgetUseCase = tentativeCommitBudgetUseCase;
        this.firmCommitBudgetUseCase = firmCommitBudgetUseCase;
        this.releaseBudgetCommitmentUseCase = releaseBudgetCommitmentUseCase;
        this.objectMapper = objectMapper;
        this.defaultGlAccountCode = defaultGlAccountCode == null || defaultGlAccountCode.isBlank()
                ? "6002"
                : defaultGlAccountCode.trim().toUpperCase();
    }

    @KafkaListener(topics = "${eprocure.finance.kafka.topics.pr-submitted:procurement.pr.submitted}")
    public void consumeSubmitted(ConsumerRecord<String, String> record) {
        PrBudgetKafkaEvent event = parse(record.value(), PrBudgetKafkaEvent.class);
        log.info("[KAFKA] Consumed PR submitted for budget | topic={} | eventId={} | prId={}",
                record.topic(),
                event.eventId(),
                LogMaskingUtil.maskId(event.payload().purchaseRequestId()));
        tentativeCommitBudgetUseCase.execute(toRecordCommand(record, event));
    }

    @KafkaListener(topics = "${eprocure.finance.kafka.topics.pr-approved:procurement.pr.approved}")
    public void consumeApproved(ConsumerRecord<String, String> record) {
        PrBudgetKafkaEvent event = parse(record.value(), PrBudgetKafkaEvent.class);
        log.info("[KAFKA] Consumed PR approved for budget | topic={} | eventId={} | prId={}",
                record.topic(),
                event.eventId(),
                LogMaskingUtil.maskId(event.payload().purchaseRequestId()));
        firmCommitBudgetUseCase.execute(toRecordCommand(record, event));
    }

    @KafkaListener(topics = {
            "${eprocure.finance.kafka.topics.pr-rejected:procurement.pr.rejected}",
            "${eprocure.finance.kafka.topics.pr-changes-requested:procurement.pr.changes-requested}"
    })
    public void consumeApprovalRelease(ConsumerRecord<String, String> record) {
        PrBudgetKafkaEvent event = parse(record.value(), PrBudgetKafkaEvent.class);
        log.info("[KAFKA] Consumed PR approval release for budget | topic={} | eventId={} | prId={}",
                record.topic(),
                event.eventId(),
                LogMaskingUtil.maskId(event.payload().purchaseRequestId()));
        releaseBudgetCommitmentUseCase.execute(toReleaseCommand(record, event.eventId(), event.timestamp(),
                event.payload().purchaseRequestId(), event.payload().prNumber()));
    }

    @KafkaListener(topics = "${eprocure.finance.kafka.topics.pr-cancelled:procurement.pr.cancelled}")
    public void consumeCancelled(ConsumerRecord<String, String> record) {
        PrCancelledKafkaEvent event = parse(record.value(), PrCancelledKafkaEvent.class);
        log.info("[KAFKA] Consumed PR cancelled for budget | topic={} | eventId={} | prId={}",
                record.topic(),
                event.eventId(),
                LogMaskingUtil.maskId(event.payload().purchaseRequestId()));
        releaseBudgetCommitmentUseCase.execute(toReleaseCommand(record, event.eventId(), event.timestamp(),
                event.payload().purchaseRequestId(), event.payload().prNumber()));
    }

    private RecordBudgetCommitmentCommand toRecordCommand(ConsumerRecord<String, String> record, PrBudgetKafkaEvent event) {
        return new RecordBudgetCommitmentCommand(
                event.eventId(),
                record.topic(),
                record.partition(),
                record.offset(),
                event.payload().purchaseRequestId(),
                event.payload().prNumber(),
                event.payload().departmentId(),
                fiscalYear(event),
                defaultGlAccountCode,
                new Money(event.payload().totalAmount().amount(), event.payload().totalAmount().currency()),
                event.timestamp());
    }

    private ReleaseBudgetCommitmentCommand toReleaseCommand(
            ConsumerRecord<String, String> record,
            String eventId,
            Instant timestamp,
            UUID purchaseRequestId,
            String prNumber) {
        return new ReleaseBudgetCommitmentCommand(
                eventId,
                record.topic(),
                record.partition(),
                record.offset(),
                purchaseRequestId,
                prNumber,
                timestamp);
    }

    private int fiscalYear(PrBudgetKafkaEvent event) {
        Integer fiscalYear = event.payload().fiscalYear();
        if (fiscalYear != null) {
            return fiscalYear;
        }
        return event.timestamp().atZone(ZoneOffset.UTC).getYear();
    }

    private <T> T parse(String value, Class<T> type) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Kafka event payload must not be blank");
        }
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Cannot parse Kafka event payload", exception);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PrBudgetKafkaEvent(
            String eventId,
            String eventType,
            String version,
            String source,
            Instant timestamp,
            UUID traceId,
            Payload payload) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Payload(
            UUID purchaseRequestId,
            String prNumber,
            UUID departmentId,
            Integer fiscalYear,
            MoneyPayload totalAmount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MoneyPayload(BigDecimal amount, String currency) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PrCancelledKafkaEvent(
            String eventId,
            String eventType,
            String version,
            String source,
            Instant timestamp,
            UUID traceId,
            CancelledPayload payload) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CancelledPayload(UUID purchaseRequestId, String prNumber) {
    }
}
