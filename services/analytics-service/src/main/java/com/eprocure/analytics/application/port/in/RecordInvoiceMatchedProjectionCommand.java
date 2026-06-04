package com.eprocure.analytics.application.port.in;

import com.eprocure.analytics.domain.model.projection.AnalyticsEventMetadata;
import com.eprocure.analytics.domain.model.projection.InvoiceMatchedProjection;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RecordInvoiceMatchedProjectionCommand(
        String eventId,
        String topic,
        int partitionId,
        long offsetValue,
        Instant eventTimestamp,
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

    public InvoiceMatchedProjection toProjection() {
        return new InvoiceMatchedProjection(
                new AnalyticsEventMetadata(eventId, topic, partitionId, offsetValue, eventTimestamp),
                invoiceId,
                invoiceNumber,
                poId,
                poNumber,
                vendorId,
                vendorName,
                totalAmount,
                currency,
                dueDate,
                matchedAt);
    }
}
