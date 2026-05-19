package com.eprocure.pr.application.port.out;

import com.eprocure.pr.domain.event.PrSubmittedEvent;

public interface PrSubmittedEventPublisher {
    void publish(PrSubmittedEvent event);
}
