package com.eprocure.inventory.application.port.out;

import com.eprocure.inventory.domain.event.GrCreatedEvent;

public interface GrCreatedEventPublisher {
    void publish(GrCreatedEvent event);
}
