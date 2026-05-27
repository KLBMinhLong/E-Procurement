package com.eprocure.approval.infrastructure.kafka;

import com.eprocure.approval.application.port.out.ApprovalStepAssignedEventPublisher;
import com.eprocure.approval.common.util.LogMaskingUtil;
import com.eprocure.approval.domain.event.ApprovalStepAssignedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.approval.integration.fallback-enabled", havingValue = "true", matchIfMissing = true)
public class LoggingApprovalStepAssignedEventPublisher implements ApprovalStepAssignedEventPublisher {
    private static final Logger log = LogManager.getLogger(LoggingApprovalStepAssignedEventPublisher.class);

    @Override
    public void publish(ApprovalStepAssignedEvent event) {
        log.info("[AUDIT] approval step assigned event | eventId={} | processId={} | stepId={} | approverId={}",
                event.eventId(),
                LogMaskingUtil.maskId(event.payload().processId()),
                LogMaskingUtil.maskId(event.payload().approvalStepId()),
                LogMaskingUtil.maskId(event.payload().approverId()));
    }
}
