package com.eprocure.pr.application.port.out;

import com.eprocure.pr.domain.event.PrCancelledEvent;

public interface PrCancelledEventPublisher {
    void publish(PrCancelledEvent event);
}
