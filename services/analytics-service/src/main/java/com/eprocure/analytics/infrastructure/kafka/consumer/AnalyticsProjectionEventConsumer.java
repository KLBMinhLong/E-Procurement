package com.eprocure.analytics.infrastructure.kafka.consumer;

import com.eprocure.analytics.application.port.in.RecordApprovalSlaBreachedProjectionCommand;
import com.eprocure.analytics.application.port.in.RecordInvoiceMatchedProjectionCommand;
import com.eprocure.analytics.application.port.in.RecordPoIssuedProjectionCommand;
import com.eprocure.analytics.application.port.in.RecordPrSubmittedProjectionCommand;
import com.eprocure.analytics.application.usecase.RecordAnalyticsProjectionUseCase;
import com.eprocure.analytics.common.util.LogMaskingUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.analytics.integration.kafka-enabled", havingValue = "true")
public class AnalyticsProjectionEventConsumer {
    private static final Logger log = LogManager.getLogger(AnalyticsProjectionEventConsumer.class);

    private final RecordAnalyticsProjectionUseCase recordAnalyticsProjectionUseCase;
    private final ObjectMapper objectMapper;

    public AnalyticsProjectionEventConsumer(
            RecordAnalyticsProjectionUseCase recordAnalyticsProjectionUseCase,
            ObjectMapper objectMapper) {
        this.recordAnalyticsProjectionUseCase = recordAnalyticsProjectionUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {
            "${eprocure.analytics.kafka.topics.pr-submitted:procurement.pr.submitted}",
            "${eprocure.analytics.kafka.topics.po-issued:procurement.po.issued}",
            "${eprocure.analytics.kafka.topics.invoice-matched:finance.invoice.matched}",
            "${eprocure.analytics.kafka.topics.approval-sla-breached:approval.sla.breached}"
    })
    public void consume(ConsumerRecord<String, String> record) {
        try {
            JsonNode root = root(record);
            String eventType = requiredText(root, "eventType");
            switch (eventType) {
                case "PURCHASE_REQUEST_SUBMITTED" -> consumePrSubmitted(record, root);
                case "PO_ISSUED" -> consumePoIssued(record, root);
                case "INVOICE_MATCHED" -> consumeInvoiceMatched(record, root);
                case "APPROVAL_SLA_BREACHED" -> consumeApprovalSlaBreached(record, root);
                default -> log.warn("[KAFKA] Skip unsupported analytics event | topic={} | eventType={}",
                        record.topic(),
                        eventType);
            }
        } catch (Exception exception) {
            log.warn("[KAFKA] Skip invalid analytics event | topic={} | reason={}",
                    record.topic(),
                    exception.getMessage());
        }
    }

    private void consumePrSubmitted(ConsumerRecord<String, String> record, JsonNode root) {
        Instant eventTimestamp = requiredTimestamp(root);
        PrSubmittedPayload payload = payload(root, PrSubmittedPayload.class);
        MoneyPayload totalAmount = money(payload.totalAmount());
        RecordPrSubmittedProjectionCommand command = new RecordPrSubmittedProjectionCommand(
                requiredText(root, "eventId"),
                record.topic(),
                record.partition(),
                record.offset(),
                eventTimestamp,
                payload.purchaseRequestId(),
                payload.prNumber(),
                payload.requesterId(),
                payload.departmentId(),
                payload.priority(),
                fiscalYear(payload.fiscalYear(), eventTimestamp),
                totalAmount.amount(),
                totalAmount.currency(),
                payload.submittedAt() == null ? eventTimestamp : payload.submittedAt());
        recordAnalyticsProjectionUseCase.recordPrSubmitted(command);
        log.info("[KAFKA] Consumed analytics PR submitted event | topic={} | eventId={} | prId={}",
                record.topic(),
                command.eventId(),
                LogMaskingUtil.maskId(command.purchaseRequestId()));
    }

    private void consumePoIssued(ConsumerRecord<String, String> record, JsonNode root) {
        PoIssuedPayload payload = payload(root, PoIssuedPayload.class);
        RecordPoIssuedProjectionCommand command = new RecordPoIssuedProjectionCommand(
                requiredText(root, "eventId"),
                record.topic(),
                record.partition(),
                record.offset(),
                requiredTimestamp(root),
                payload.poId(),
                payload.poNumber(),
                payload.prId(),
                payload.prNumber(),
                payload.vendorId(),
                payload.vendorName(),
                payload.totalAmount(),
                payload.currency(),
                payload.issuedAt(),
                safeList(payload.lineItems()).stream()
                        .map(item -> new RecordPoIssuedProjectionCommand.LineItem(
                                item.poLineItemId(),
                                item.prLineItemId(),
                                item.itemName(),
                                item.categoryCode(),
                                item.quantity(),
                                item.unit(),
                                item.unitPrice(),
                                item.totalPrice(),
                                item.currency()))
                        .toList());
        recordAnalyticsProjectionUseCase.recordPoIssued(command);
        log.info("[KAFKA] Consumed analytics PO issued event | topic={} | eventId={} | poId={}",
                record.topic(),
                command.eventId(),
                LogMaskingUtil.maskId(command.poId()));
    }

    private void consumeInvoiceMatched(ConsumerRecord<String, String> record, JsonNode root) {
        InvoiceMatchedPayload payload = payload(root, InvoiceMatchedPayload.class);
        RecordInvoiceMatchedProjectionCommand command = new RecordInvoiceMatchedProjectionCommand(
                requiredText(root, "eventId"),
                record.topic(),
                record.partition(),
                record.offset(),
                requiredTimestamp(root),
                payload.invoiceId(),
                payload.invoiceNumber(),
                payload.poId(),
                payload.poNumber(),
                payload.vendorId(),
                payload.vendorName(),
                payload.totalAmount(),
                payload.currency(),
                payload.dueDate(),
                payload.matchedAt());
        recordAnalyticsProjectionUseCase.recordInvoiceMatched(command);
        log.info("[KAFKA] Consumed analytics invoice matched event | topic={} | eventId={} | invoiceId={}",
                record.topic(),
                command.eventId(),
                LogMaskingUtil.maskId(command.invoiceId()));
    }

    private void consumeApprovalSlaBreached(ConsumerRecord<String, String> record, JsonNode root) {
        ApprovalSlaBreachedPayload payload = payload(root, ApprovalSlaBreachedPayload.class);
        RecordApprovalSlaBreachedProjectionCommand command = new RecordApprovalSlaBreachedProjectionCommand(
                requiredText(root, "eventId"),
                record.topic(),
                record.partition(),
                record.offset(),
                requiredTimestamp(root),
                payload.processId(),
                payload.approvalStepId(),
                payload.purchaseRequestId(),
                payload.prNumber(),
                payload.priority(),
                payload.stepIndex(),
                payload.stepType(),
                payload.approverRole(),
                payload.breachedApproverId(),
                payload.escalatedToApproverId(),
                payload.reassigned(),
                payload.assignedAt(),
                payload.slaDeadline(),
                payload.breachedAt());
        recordAnalyticsProjectionUseCase.recordApprovalSlaBreached(command);
        log.info("[KAFKA] Consumed analytics SLA breach event | topic={} | eventId={} | approvalStepId={}",
                record.topic(),
                command.eventId(),
                LogMaskingUtil.maskId(command.approvalStepId()));
    }

    private JsonNode root(ConsumerRecord<String, String> record) throws JsonProcessingException {
        if (record.value() == null || record.value().isBlank()) {
            throw new IllegalArgumentException("Kafka event payload must not be blank");
        }
        return objectMapper.readTree(record.value());
    }

    private <T> T payload(JsonNode root, Class<T> type) {
        JsonNode payload = root.path("payload");
        if (payload.isMissingNode() || payload.isNull()) {
            throw new IllegalArgumentException("payload is required");
        }
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Cannot parse payload", exception);
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

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private int fiscalYear(Integer fiscalYear, Instant eventTimestamp) {
        return fiscalYear == null ? eventTimestamp.atZone(java.time.ZoneOffset.UTC).getYear() : fiscalYear;
    }

    private MoneyPayload money(MoneyPayload value) {
        if (value == null) {
            return new MoneyPayload(BigDecimal.ZERO, "VND");
        }
        return value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PrSubmittedPayload(
            UUID purchaseRequestId,
            String prNumber,
            UUID requesterId,
            UUID departmentId,
            String priority,
            Integer fiscalYear,
            MoneyPayload totalAmount,
            Instant submittedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MoneyPayload(
            BigDecimal amount,
            String currency) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PoIssuedPayload(
            UUID poId,
            String poNumber,
            UUID prId,
            String prNumber,
            UUID vendorId,
            String vendorName,
            BigDecimal totalAmount,
            String currency,
            Instant issuedAt,
            List<PoIssuedLineItem> lineItems) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PoIssuedLineItem(
            UUID poLineItemId,
            UUID prLineItemId,
            String itemName,
            String categoryCode,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String currency) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record InvoiceMatchedPayload(
            UUID invoiceId,
            String invoiceNumber,
            UUID poId,
            String poNumber,
            UUID vendorId,
            String vendorName,
            BigDecimal totalAmount,
            String currency,
            LocalDate dueDate,
            Instant matchedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ApprovalSlaBreachedPayload(
            UUID processId,
            UUID approvalStepId,
            UUID purchaseRequestId,
            String prNumber,
            String priority,
            int stepIndex,
            String stepType,
            String approverRole,
            UUID breachedApproverId,
            UUID escalatedToApproverId,
            boolean reassigned,
            Instant assignedAt,
            Instant slaDeadline,
            Instant breachedAt) {
    }
}
