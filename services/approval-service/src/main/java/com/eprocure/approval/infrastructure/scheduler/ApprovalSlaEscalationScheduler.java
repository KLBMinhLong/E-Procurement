package com.eprocure.approval.infrastructure.scheduler;

import com.eprocure.approval.application.usecase.SlaEscalationUseCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.approval.sla.escalation-enabled", havingValue = "true", matchIfMissing = true)
public class ApprovalSlaEscalationScheduler {
    private static final Logger log = LogManager.getLogger(ApprovalSlaEscalationScheduler.class);

    private final SlaEscalationUseCase slaEscalationUseCase;

    public ApprovalSlaEscalationScheduler(SlaEscalationUseCase slaEscalationUseCase) {
        this.slaEscalationUseCase = slaEscalationUseCase;
    }

    @Scheduled(fixedDelayString = "#{${eprocure.approval.sla.check-interval-minutes:15} * 60 * 1000}")
    public void checkOverdueApprovalSteps() {
        try {
            slaEscalationUseCase.execute();
        } catch (RuntimeException exception) {
            log.error("[EXCEPTION][APR_SLA] SLA escalation scan failed | error={}", exception.getMessage(), exception);
        }
    }
}
