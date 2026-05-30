package com.eprocure.pr.application.port.out;

import com.eprocure.pr.domain.event.PrApprovalResultEvent;

public interface PrApprovalResultEventPublisher {
    void publish(PrApprovalResultEvent event);
}
