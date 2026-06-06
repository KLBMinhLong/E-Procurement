package com.eprocure.analytics.application.port.in;

import com.eprocure.analytics.domain.model.projection.AnalyticsEventMetadata;
import com.eprocure.analytics.domain.model.projection.ApprovalStepAssignedProjection;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RecordApprovalStepAssignedProjectionCommand(
        String eventId,
        String topic,
        int partition,
        long offset,
        Instant eventTimestamp,
        UUID processId,
        UUID approvalStepId,
        UUID purchaseRequestId,
        String prNumber,
        String priority,
        int stepIndex,
        String stepType,
        String approverRole,
        UUID approverId,
        Instant assignedAt,
        Instant slaDeadline) {

    public RecordApprovalStepAssignedProjectionCommand {
        eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        topic = Objects.requireNonNull(topic, "topic must not be null");
        eventTimestamp = Objects.requireNonNull(eventTimestamp, "eventTimestamp must not be null");
    }

    public ApprovalStepAssignedProjection toProjection() {
        return new ApprovalStepAssignedProjection(
                new AnalyticsEventMetadata(eventId, topic, partition, offset, eventTimestamp),
                processId,
                approvalStepId,
                purchaseRequestId,
                prNumber,
                priority,
                stepIndex,
                stepType,
                approverRole,
                approverId,
                assignedAt,
                slaDeadline);
    }
}
