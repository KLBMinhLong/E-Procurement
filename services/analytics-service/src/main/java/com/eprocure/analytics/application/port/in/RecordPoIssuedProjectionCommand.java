package com.eprocure.analytics.application.port.in;

import com.eprocure.analytics.domain.model.projection.AnalyticsEventMetadata;
import com.eprocure.analytics.domain.model.projection.PoIssuedLineProjection;
import com.eprocure.analytics.domain.model.projection.PoIssuedProjection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecordPoIssuedProjectionCommand(
        String eventId,
        String topic,
        int partitionId,
        long offsetValue,
        Instant eventTimestamp,
        UUID poId,
        String poNumber,
        UUID prId,
        String prNumber,
        UUID vendorId,
        String vendorName,
        BigDecimal totalAmount,
        String currency,
        Instant issuedAt,
        List<LineItem> lineItems) {

    public RecordPoIssuedProjectionCommand {
        lineItems = List.copyOf(lineItems == null ? List.of() : lineItems);
    }

    public PoIssuedProjection toProjection() {
        return new PoIssuedProjection(
                new AnalyticsEventMetadata(eventId, topic, partitionId, offsetValue, eventTimestamp),
                poId,
                poNumber,
                prId,
                prNumber,
                vendorId,
                vendorName,
                totalAmount,
                currency,
                issuedAt,
                lineItems.stream()
                        .map(LineItem::toProjection)
                        .toList());
    }

    public record LineItem(
            UUID poLineItemId,
            UUID prLineItemId,
            String itemName,
            String categoryCode,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String currency) {

        PoIssuedLineProjection toProjection() {
            return new PoIssuedLineProjection(
                    poLineItemId,
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
