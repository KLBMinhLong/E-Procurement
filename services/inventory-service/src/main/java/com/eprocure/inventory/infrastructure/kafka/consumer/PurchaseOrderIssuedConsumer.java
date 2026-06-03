package com.eprocure.inventory.infrastructure.kafka.consumer;

import com.eprocure.inventory.application.port.in.RecordIssuedPurchaseOrderCommand;
import com.eprocure.inventory.application.usecase.RecordIssuedPurchaseOrderUseCase;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
@ConditionalOnProperty(name = "eprocure.inventory.integration.kafka-enabled", havingValue = "true")
public class PurchaseOrderIssuedConsumer {
    private static final Logger log = LogManager.getLogger(PurchaseOrderIssuedConsumer.class);

    private final RecordIssuedPurchaseOrderUseCase recordIssuedPurchaseOrderUseCase;
    private final ObjectMapper objectMapper;

    public PurchaseOrderIssuedConsumer(
            RecordIssuedPurchaseOrderUseCase recordIssuedPurchaseOrderUseCase,
            ObjectMapper objectMapper) {
        this.recordIssuedPurchaseOrderUseCase = recordIssuedPurchaseOrderUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${eprocure.inventory.kafka.topics.po-issued:procurement.po.issued}")
    public void consumeIssued(ConsumerRecord<String, String> record) {
        PurchaseOrderIssuedKafkaEvent event;
        RecordIssuedPurchaseOrderCommand command;
        try {
            event = parse(record.value(), PurchaseOrderIssuedKafkaEvent.class);
            validate(event);
            command = toCommand(record, event);
        } catch (IllegalArgumentException exception) {
            log.warn("[KAFKA] Skip invalid PO issued event | topic={} | reason={}",
                    record.topic(),
                    exception.getMessage());
            return;
        }

        log.info("[KAFKA] Consumed PO issued for inventory snapshot | topic={} | eventId={} | poId={} | poNumber={}",
                record.topic(),
                event.eventId(),
                LogMaskingUtil.maskId(event.payload().poId()),
                event.payload().poNumber());
        recordIssuedPurchaseOrderUseCase.execute(command);
    }

    private RecordIssuedPurchaseOrderCommand toCommand(
            ConsumerRecord<String, String> record,
            PurchaseOrderIssuedKafkaEvent event) {
        PurchaseOrderIssuedPayload payload = event.payload();
        return new RecordIssuedPurchaseOrderCommand(
                event.eventId(),
                record.topic(),
                record.partition(),
                record.offset(),
                event.timestamp(),
                payload.poId(),
                payload.poNumber(),
                payload.prId(),
                payload.prNumber(),
                payload.vendorId(),
                payload.vendorName(),
                payload.vendorEmail(),
                payload.vendorTaxCode(),
                payload.purchasingOfficerId(),
                payload.totalAmount(),
                payload.currency(),
                payload.deliveryAddress(),
                payload.deliveryDeadline(),
                payload.paymentTerms(),
                payload.issuedAt(),
                payload.sentToVendorAt(),
                payload.lineItems().stream()
                        .map(item -> new RecordIssuedPurchaseOrderCommand.LineItem(
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

    private void validate(PurchaseOrderIssuedKafkaEvent event) {
        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (event.timestamp() == null) {
            throw new IllegalArgumentException("timestamp is required");
        }
        if (event.payload() == null) {
            throw new IllegalArgumentException("payload is required");
        }
        if (event.payload().lineItems() == null || event.payload().lineItems().isEmpty()) {
            throw new IllegalArgumentException("payload.lineItems is required");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PurchaseOrderIssuedKafkaEvent(
            String eventId,
            String eventType,
            String version,
            String source,
            Instant timestamp,
            UUID traceId,
            PurchaseOrderIssuedPayload payload) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PurchaseOrderIssuedPayload(
            UUID poId,
            String poNumber,
            UUID prId,
            String prNumber,
            UUID vendorId,
            String vendorName,
            String vendorEmail,
            String vendorTaxCode,
            UUID purchasingOfficerId,
            BigDecimal totalAmount,
            String currency,
            String deliveryAddress,
            LocalDate deliveryDeadline,
            String paymentTerms,
            Instant issuedAt,
            Instant sentToVendorAt,
            List<PurchaseOrderIssuedLineItem> lineItems) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PurchaseOrderIssuedLineItem(
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
}
