package com.eprocure.approval.infrastructure.kafka;

import com.eprocure.approval.application.port.out.ApprovalSlaBreachedEventPublisher;
import com.eprocure.approval.common.util.LogMaskingUtil;
import com.eprocure.approval.domain.event.ApprovalSlaBreachedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.approval.integration.fallback-enabled", havingValue = "true", matchIfMissing = true)
public class LoggingApprovalSlaBreachedEventPublisher implements ApprovalSlaBreachedEventPublisher {
    private static final Logger log = LogManager.getLogger(LoggingApprovalSlaBreachedEventPublisher.class);

    @Override
    public void publish(ApprovalSlaBreachedEvent event) {
        log.info("[AUDIT] approval sla breached event | eventId={} | processId={} | stepId={} | escalatedToApproverId={}",
                event.eventId(),
                LogMaskingUtil.maskId(event.payload().processId()),
                LogMaskingUtil.maskId(event.payload().approvalStepId()),
                LogMaskingUtil.maskId(event.payload().escalatedToApproverId()));
    }
}
