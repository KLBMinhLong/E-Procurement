package com.eprocure.analytics.application.port.in;

import com.eprocure.analytics.domain.model.projection.AnalyticsEventMetadata;
import com.eprocure.analytics.domain.model.projection.PrSubmittedProjection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecordPrSubmittedProjectionCommand(
        String eventId,
        String topic,
        int partitionId,
        long offsetValue,
        Instant eventTimestamp,
        UUID purchaseRequestId,
        String prNumber,
        UUID requesterId,
        UUID departmentId,
        String priority,
        int fiscalYear,
        BigDecimal totalAmount,
        String currency,
        Instant submittedAt) {

    public PrSubmittedProjection toProjection() {
        return new PrSubmittedProjection(
                new AnalyticsEventMetadata(eventId, topic, partitionId, offsetValue, eventTimestamp),
                purchaseRequestId,
                prNumber,
                requesterId,
                departmentId,
                priority,
                fiscalYear,
                totalAmount,
                currency,
                submittedAt);
    }
}
