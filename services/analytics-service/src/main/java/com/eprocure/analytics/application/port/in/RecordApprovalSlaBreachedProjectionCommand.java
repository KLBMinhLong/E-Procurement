package com.eprocure.analytics.application.port.in;

import com.eprocure.analytics.domain.model.projection.AnalyticsEventMetadata;
import com.eprocure.analytics.domain.model.projection.ApprovalSlaBreachProjection;
import java.time.Instant;
import java.util.UUID;

public record RecordApprovalSlaBreachedProjectionCommand(
        String eventId,
        String topic,
        int partitionId,
        long offsetValue,
        Instant eventTimestamp,
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

    public ApprovalSlaBreachProjection toProjection() {
        return new ApprovalSlaBreachProjection(
                new AnalyticsEventMetadata(eventId, topic, partitionId, offsetValue, eventTimestamp),
                processId,
                approvalStepId,
                purchaseRequestId,
                prNumber,
                priority,
                stepIndex,
                stepType,
                approverRole,
                breachedApproverId,
                escalatedToApproverId,
                reassigned,
                assignedAt,
                slaDeadline,
                breachedAt);
    }
}
