package com.eprocure.admin.infrastructure.scheduler;

import com.eprocure.admin.application.usecase.ProcessAuditExportJobsUseCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.admin.audit-export-worker.enabled", havingValue = "true", matchIfMissing = true)
public class AuditExportWorkerScheduler {
    private static final Logger log = LogManager.getLogger(AuditExportWorkerScheduler.class);

    private final ProcessAuditExportJobsUseCase processAuditExportJobsUseCase;
    private final int batchSize;

    public AuditExportWorkerScheduler(
            ProcessAuditExportJobsUseCase processAuditExportJobsUseCase,
            @Value("${eprocure.admin.audit-export-worker.batch-size:5}") int batchSize) {
        this.processAuditExportJobsUseCase = processAuditExportJobsUseCase;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${eprocure.admin.audit-export-worker.poll-delay-ms:60000}")
    public void processQueuedAuditExportJobs() {
        try {
            int processed = processAuditExportJobsUseCase.execute(batchSize);
            if (processed > 0) {
                log.info("[ACTION] Audit export worker processed | count={}", processed);
            }
        } catch (RuntimeException exception) {
            log.error("[EXCEPTION][ADM_AUDIT_EXPORT] Audit export worker failed | error={}",
                    exception.getMessage(),
                    exception);
        }
    }
}
