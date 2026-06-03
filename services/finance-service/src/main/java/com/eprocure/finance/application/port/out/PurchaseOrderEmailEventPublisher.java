package com.eprocure.finance.application.port.out;

import com.eprocure.finance.domain.event.PurchaseOrderEmailRequestedEvent;

public interface PurchaseOrderEmailEventPublisher {
    void publish(PurchaseOrderEmailRequestedEvent event);
}
