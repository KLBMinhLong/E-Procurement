package com.eprocure.finance.infrastructure.kafka.consumer;

import com.eprocure.finance.application.port.in.RecordGoodsReceiptSnapshotCommand;
import com.eprocure.finance.application.usecase.RecordGoodsReceiptSnapshotUseCase;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.GoodsReceiptLineSnapshot;
import com.eprocure.finance.domain.model.GoodsReceiptSnapshotStatus;
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
public class GoodsReceiptCreatedConsumer {
    private static final Logger log = LogManager.getLogger(GoodsReceiptCreatedConsumer.class);

    private final RecordGoodsReceiptSnapshotUseCase recordGoodsReceiptSnapshotUseCase;
    private final ObjectMapper objectMapper;

    public GoodsReceiptCreatedConsumer(
            RecordGoodsReceiptSnapshotUseCase recordGoodsReceiptSnapshotUseCase,
            ObjectMapper objectMapper) {
        this.recordGoodsReceiptSnapshotUseCase = recordGoodsReceiptSnapshotUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${eprocure.finance.kafka.topics.gr-created:inventory.gr.created}")
    public void consume(ConsumerRecord<String, String> record) {
        GrCreatedKafkaEvent event = parse(record.value(), GrCreatedKafkaEvent.class);
        log.info("[KAFKA] Consumed GR created for invoice match | topic={} | eventId={} | grId={} | poId={}",
                record.topic(),
                event.eventId(),
                LogMaskingUtil.maskId(event.payload().grId()),
                LogMaskingUtil.maskId(event.payload().poId()));
        recordGoodsReceiptSnapshotUseCase.execute(toCommand(record, event));
    }

    private RecordGoodsReceiptSnapshotCommand toCommand(
            ConsumerRecord<String, String> record,
            GrCreatedKafkaEvent event) {
        GrCreatedPayload payload = event.payload();
        return new RecordGoodsReceiptSnapshotCommand(
                event.eventId(),
                record.topic(),
                record.partition(),
                record.offset(),
                payload.grId(),
                payload.grNumber(),
                payload.poId(),
                payload.poNumber(),
                payload.warehouseId(),
                payload.warehouseKeeperId(),
                payload.status(),
                payload.receivedAt(),
                payload.completedAt(),
                payload.lineItems().stream()
                        .map(item -> new GoodsReceiptLineSnapshot(
                                item.grLineItemId(),
                                item.poLineItemId(),
                                item.itemCode(),
                                item.itemName(),
                                item.orderedQuantity(),
                                item.receivedQuantity(),
                                item.rejectedQuantity(),
                                item.unit()))
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
    record GrCreatedKafkaEvent(
            String eventId,
            String eventType,
            String version,
            String source,
            Instant timestamp,
            UUID traceId,
            GrCreatedPayload payload) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GrCreatedPayload(
            UUID grId,
            String grNumber,
            UUID poId,
            String poNumber,
            UUID warehouseId,
            UUID warehouseKeeperId,
            GoodsReceiptSnapshotStatus status,
            Instant receivedAt,
            Instant completedAt,
            List<GrCreatedLineItem> lineItems) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GrCreatedLineItem(
            UUID grLineItemId,
            UUID poLineItemId,
            String itemCode,
            String itemName,
            BigDecimal orderedQuantity,
            BigDecimal receivedQuantity,
            BigDecimal rejectedQuantity,
            String unit) {
    }
}
