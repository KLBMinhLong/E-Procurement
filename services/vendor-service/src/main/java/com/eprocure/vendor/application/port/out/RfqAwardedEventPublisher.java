package com.eprocure.vendor.application.port.out;

import com.eprocure.vendor.domain.event.RfqAwardedEvent;

public interface RfqAwardedEventPublisher {
    void publish(RfqAwardedEvent event);
}
