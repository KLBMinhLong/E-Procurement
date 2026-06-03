package com.eprocure.finance.infrastructure.kafka.consumer;

import com.eprocure.finance.application.port.in.CreatePurchaseOrderFromRfqAwardCommand;
import com.eprocure.finance.application.usecase.CreatePurchaseOrderFromRfqAwardUseCase;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.vo.Money;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.finance.integration.kafka-enabled", havingValue = "true")
public class RfqAwardedPurchaseOrderConsumer {
    private static final Logger log = LogManager.getLogger(RfqAwardedPurchaseOrderConsumer.class);

    private final CreatePurchaseOrderFromRfqAwardUseCase createPurchaseOrderFromRfqAwardUseCase;
    private final ObjectMapper objectMapper;

    public RfqAwardedPurchaseOrderConsumer(
            CreatePurchaseOrderFromRfqAwardUseCase createPurchaseOrderFromRfqAwardUseCase,
            ObjectMapper objectMapper) {
        this.createPurchaseOrderFromRfqAwardUseCase = createPurchaseOrderFromRfqAwardUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${eprocure.finance.kafka.topics.rfq-awarded:procurement.rfq.awarded}")
    public void consumeAwarded(ConsumerRecord<String, String> record) {
        RfqAwardedKafkaEvent event = parse(record.value(), RfqAwardedKafkaEvent.class);
        log.info("[KAFKA] Consumed RFQ awarded for PO | topic={} | eventId={} | rfqId={} | prId={}",
                record.topic(),
                event.eventId(),
                LogMaskingUtil.maskId(event.payload().rfqId()),
                LogMaskingUtil.maskId(event.payload().prId()));
        createPurchaseOrderFromRfqAwardUseCase.execute(toCommand(record, event));
    }

    private CreatePurchaseOrderFromRfqAwardCommand toCommand(
            ConsumerRecord<String, String> record,
            RfqAwardedKafkaEvent event) {
        RfqAwardedPayload payload = event.payload();
        return new CreatePurchaseOrderFromRfqAwardCommand(
                event.eventId(),
                record.topic(),
                record.partition(),
                record.offset(),
                payload.rfqId(),
                payload.rfqNumber(),
                payload.prId(),
                payload.prNumber(),
                payload.vendorId(),
                payload.vendorName(),
                payload.vendorEmail(),
                payload.vendorTaxCode(),
                payload.awardedQuoteId(),
                new Money(payload.totalAmount(), payload.currency()),
                payload.paymentTerms(),
                payload.awardedBy(),
                event.timestamp(),
                payload.lineItems().stream()
                        .map(item -> new CreatePurchaseOrderFromRfqAwardCommand.LineItem(
                                item.rfqLineItemId(),
                                item.prLineItemId(),
                                item.itemName(),
                                item.categoryCode(),
                                item.quantity(),
                                item.unit(),
                                new Money(item.unitPrice(), item.currency()),
                                new Money(item.totalPrice(), item.currency()),
                                item.deliveryDays(),
                                item.warranty()))
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RfqAwardedKafkaEvent(
            String eventId,
            String eventType,
            String version,
            String source,
            Instant timestamp,
            UUID traceId,
            RfqAwardedPayload payload) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RfqAwardedPayload(
            UUID rfqId,
            String rfqNumber,
            UUID prId,
            String prNumber,
            UUID vendorId,
            String vendorName,
            String vendorEmail,
            String vendorTaxCode,
            UUID awardedQuoteId,
            BigDecimal totalAmount,
            String currency,
            String paymentTerms,
            UUID awardedBy,
            List<RfqAwardedLineItem> lineItems) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RfqAwardedLineItem(
            UUID rfqLineItemId,
            UUID prLineItemId,
            String itemName,
            String categoryCode,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String currency,
            Integer deliveryDays,
            String warranty) {
    }
}
