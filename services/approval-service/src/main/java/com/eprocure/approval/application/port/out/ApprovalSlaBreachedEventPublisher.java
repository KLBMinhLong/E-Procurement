package com.eprocure.approval.application.port.out;

import com.eprocure.approval.domain.event.ApprovalSlaBreachedEvent;

public interface ApprovalSlaBreachedEventPublisher {
    void publish(ApprovalSlaBreachedEvent event);
}
