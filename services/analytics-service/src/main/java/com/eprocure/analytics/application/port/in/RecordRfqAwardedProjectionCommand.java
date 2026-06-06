package com.eprocure.analytics.application.port.in;

import com.eprocure.analytics.domain.model.projection.AnalyticsEventMetadata;
import com.eprocure.analytics.domain.model.projection.RfqAwardedLineProjection;
import com.eprocure.analytics.domain.model.projection.RfqAwardedProjection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecordRfqAwardedProjectionCommand(
        String eventId,
        String topic,
        int partitionId,
        long offsetValue,
        Instant eventTimestamp,
        UUID rfqId,
        String rfqNumber,
        UUID prId,
        String prNumber,
        UUID vendorId,
        String vendorName,
        BigDecimal totalAmount,
        String currency,
        Instant awardedAt,
        List<LineItem> lineItems) {

    public RecordRfqAwardedProjectionCommand {
        lineItems = List.copyOf(lineItems == null ? List.of() : lineItems);
    }

    public RfqAwardedProjection toProjection() {
        return new RfqAwardedProjection(
                new AnalyticsEventMetadata(eventId, topic, partitionId, offsetValue, eventTimestamp),
                rfqId,
                rfqNumber,
                prId,
                prNumber,
                vendorId,
                vendorName,
                totalAmount,
                currency,
                awardedAt,
                lineItems.stream()
                        .map(LineItem::toProjection)
                        .toList());
    }

    public record LineItem(
            UUID rfqLineItemId,
            UUID prLineItemId,
            String itemName,
            String categoryCode,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String currency) {

        RfqAwardedLineProjection toProjection() {
            return new RfqAwardedLineProjection(
                    rfqLineItemId,
                    prLineItemId,
                    itemName,
                    categoryCode,
                    quantity,
                    unit,
                    unitPrice,
                    totalPrice,
                    currency);
        }
    }
}
