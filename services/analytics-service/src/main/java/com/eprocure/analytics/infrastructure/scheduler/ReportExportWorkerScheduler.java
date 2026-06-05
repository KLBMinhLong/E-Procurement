package com.eprocure.analytics.infrastructure.scheduler;

import com.eprocure.analytics.application.usecase.ProcessReportExportJobsUseCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.analytics.report-worker.enabled", havingValue = "true", matchIfMissing = true)
public class ReportExportWorkerScheduler {
    private static final Logger log = LogManager.getLogger(ReportExportWorkerScheduler.class);

    private final ProcessReportExportJobsUseCase processReportExportJobsUseCase;
    private final int batchSize;

    public ReportExportWorkerScheduler(
            ProcessReportExportJobsUseCase processReportExportJobsUseCase,
            @Value("${eprocure.analytics.report-worker.batch-size:5}") int batchSize) {
        this.processReportExportJobsUseCase = processReportExportJobsUseCase;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${eprocure.analytics.report-worker.poll-delay-ms:60000}")
    public void processQueuedReportJobs() {
        try {
            int processed = processReportExportJobsUseCase.execute(batchSize);
            if (processed > 0) {
                log.info("[ACTION] Report export worker processed | count={}", processed);
            }
        } catch (RuntimeException exception) {
            log.error("[EXCEPTION][ANL_REPORT] Report export worker failed | error={}",
                    exception.getMessage(),
                    exception);
        }
    }
}
