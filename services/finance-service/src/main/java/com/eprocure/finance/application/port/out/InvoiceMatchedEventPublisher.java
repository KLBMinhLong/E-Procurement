package com.eprocure.finance.application.port.out;

import com.eprocure.finance.domain.event.InvoiceMatchedEvent;

public interface InvoiceMatchedEventPublisher {
    void publish(InvoiceMatchedEvent event);
}
