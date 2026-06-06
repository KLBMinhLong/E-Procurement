package com.eprocure.analytics.application.port.in;

import com.eprocure.analytics.domain.model.projection.AnalyticsEventMetadata;
import com.eprocure.analytics.domain.model.projection.GoodsReceiptCreatedProjection;
import com.eprocure.analytics.domain.model.projection.GoodsReceiptLineProjection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecordGoodsReceiptCreatedProjectionCommand(
        String eventId,
        String topic,
        int partitionId,
        long offsetValue,
        Instant eventTimestamp,
        UUID grId,
        String grNumber,
        UUID poId,
        String poNumber,
        UUID warehouseId,
        UUID warehouseKeeperId,
        String status,
        Instant receivedAt,
        Instant completedAt,
        List<LineItem> lineItems) {

    public RecordGoodsReceiptCreatedProjectionCommand {
        lineItems = List.copyOf(lineItems == null ? List.of() : lineItems);
    }

    public GoodsReceiptCreatedProjection toProjection() {
        return new GoodsReceiptCreatedProjection(
                new AnalyticsEventMetadata(eventId, topic, partitionId, offsetValue, eventTimestamp),
                grId,
                grNumber,
                poId,
                poNumber,
                warehouseId,
                warehouseKeeperId,
                status,
                receivedAt,
                completedAt,
                lineItems.stream()
                        .map(LineItem::toProjection)
                        .toList());
    }

    public record LineItem(
            UUID grLineItemId,
            UUID poLineItemId,
            String itemCode,
            String itemName,
            BigDecimal orderedQuantity,
            BigDecimal receivedQuantity,
            BigDecimal rejectedQuantity,
            String unit) {

        GoodsReceiptLineProjection toProjection() {
            return new GoodsReceiptLineProjection(
                    grLineItemId,
                    poLineItemId,
                    itemCode,
                    itemName,
                    orderedQuantity,
                    receivedQuantity,
                    rejectedQuantity,
                    unit);
        }
    }
}
