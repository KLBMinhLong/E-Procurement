package com.eprocure.finance.infrastructure.kafka;

import com.eprocure.finance.application.port.out.InvoiceMatchedEventPublisher;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.event.InvoiceMatchedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.finance.integration.kafka-enabled", havingValue = "false", matchIfMissing = true)
public class LoggingInvoiceMatchedEventPublisher implements InvoiceMatchedEventPublisher {
    private static final Logger log = LogManager.getLogger(LoggingInvoiceMatchedEventPublisher.class);

    @Override
    public void publish(InvoiceMatchedEvent event) {
        log.info("[AUDIT] invoice matched event | eventId={} | invoiceId={} | poId={}",
                event.eventId(),
                LogMaskingUtil.maskId(event.payload().invoiceId()),
                LogMaskingUtil.maskId(event.payload().poId()));
    }
}
