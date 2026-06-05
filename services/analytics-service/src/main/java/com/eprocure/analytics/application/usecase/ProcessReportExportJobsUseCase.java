package com.eprocure.analytics.application.usecase;

import com.eprocure.analytics.application.port.out.RenderedReport;
import com.eprocure.analytics.application.port.out.ReportFileRenderer;
import com.eprocure.analytics.common.util.LogMaskingUtil;
import com.eprocure.analytics.domain.model.report.ReportJob;
import com.eprocure.analytics.domain.repository.ReportJobRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessReportExportJobsUseCase {
    private static final Logger log = LogManager.getLogger(ProcessReportExportJobsUseCase.class);
    private static final int MAX_BATCH_SIZE = 50;
    private static final long DEFAULT_EXPIRY_SECONDS = 604800;
    private static final int MAX_FAILURE_REASON_LENGTH = 500;

    private final ReportJobRepository repository;
    private final ReportFileRenderer renderer;
    private final Clock clock;

    public ProcessReportExportJobsUseCase(
            ReportJobRepository repository,
            ReportFileRenderer renderer,
            Clock clock) {
        this.repository = repository;
        this.renderer = renderer;
        this.clock = clock;
    }

    @Transactional
    public int execute(int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, MAX_BATCH_SIZE));
        List<ReportJob> jobs = repository.claimQueuedForProcessing(limit, Instant.now(clock));
        if (jobs.isEmpty()) {
            return 0;
        }
        log.info("[ACTION] Start ProcessReportExportJobs | count={}", jobs.size());
        for (ReportJob job : jobs) {
            processOne(job);
        }
        log.info("[ACTION] Complete ProcessReportExportJobs | count={}", jobs.size());
        return jobs.size();
    }

    private void processOne(ReportJob job) {
        try {
            RenderedReport renderedReport = renderer.render(job);
            Instant completedAt = Instant.now(clock);
            repository.markCompleted(
                    job.id(),
                    renderedReport.downloadUrl(),
                    renderedReport.storagePath(),
                    completedAt,
                    completedAt.plusSeconds(DEFAULT_EXPIRY_SECONDS));
            log.info("[ACTION] Complete ReportExportJob | jobId={} | reportType={} | format={}",
                    LogMaskingUtil.maskId(job.id()),
                    job.reportType(),
                    job.format());
        } catch (RuntimeException exception) {
            Instant failedAt = Instant.now(clock);
            repository.markFailed(job.id(), failureReason(exception), failedAt);
            log.error("[EXCEPTION][ANL_REPORT] Report export job failed | jobId={} | reportType={} | format={} | error={}",
                    LogMaskingUtil.maskId(job.id()),
                    job.reportType(),
                    job.format(),
                    exception.getMessage(),
                    exception);
        }
    }

    private String failureReason(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() <= MAX_FAILURE_REASON_LENGTH
                ? message
                : message.substring(0, MAX_FAILURE_REASON_LENGTH);
    }
}
