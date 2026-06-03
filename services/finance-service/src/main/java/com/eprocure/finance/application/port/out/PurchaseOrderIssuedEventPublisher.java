package com.eprocure.finance.application.port.out;

import com.eprocure.finance.domain.event.PurchaseOrderIssuedEvent;

public interface PurchaseOrderIssuedEventPublisher {
    void publish(PurchaseOrderIssuedEvent event);
}
