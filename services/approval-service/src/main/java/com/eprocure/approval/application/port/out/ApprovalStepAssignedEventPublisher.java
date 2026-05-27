package com.eprocure.approval.application.port.out;

import com.eprocure.approval.domain.event.ApprovalStepAssignedEvent;

public interface ApprovalStepAssignedEventPublisher {
    void publish(ApprovalStepAssignedEvent event);
}
