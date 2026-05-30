package com.eprocure.pr.infrastructure.kafka;

import com.eprocure.pr.application.port.out.PrApprovalResultEventPublisher;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.domain.event.PrApprovalResultEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.pr.integration.fallback-enabled", havingValue = "true", matchIfMissing = true)
public class LoggingPrApprovalResultEventPublisher implements PrApprovalResultEventPublisher {
    private static final Logger log = LogManager.getLogger(LoggingPrApprovalResultEventPublisher.class);

    @Override
    public void publish(PrApprovalResultEvent event) {
        log.info("[AUDIT] pr approval result event | eventId={} | prId={} | prNumber={} | status={}",
                event.eventId(),
                LogMaskingUtil.maskId(event.payload().purchaseRequestId()),
                event.payload().prNumber(),
                event.payload().status());
    }
}
