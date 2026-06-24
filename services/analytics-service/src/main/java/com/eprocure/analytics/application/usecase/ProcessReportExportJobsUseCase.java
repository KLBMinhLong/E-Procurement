package com.eprocure.analytics.application.usecase;

import com.eprocure.analytics.application.port.out.RenderedReport;
import com.eprocure.analytics.application.port.out.ReportDataset;
import com.eprocure.analytics.application.port.out.ReportDatasetProvider;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ProcessReportExportJobsUseCase {
    private static final Logger log = LogManager.getLogger(ProcessReportExportJobsUseCase.class);
    private static final int MAX_BATCH_SIZE = 50;
    private static final long DEFAULT_EXPIRY_SECONDS = 604800;
    private static final int MAX_FAILURE_REASON_LENGTH = 500;

    private final ReportJobRepository repository;
    private final ReportDatasetProvider datasetProvider;
    private final ReportFileRenderer renderer;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public ProcessReportExportJobsUseCase(
            ReportJobRepository repository,
            ReportDatasetProvider datasetProvider,
            ReportFileRenderer renderer,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.datasetProvider = datasetProvider;
        this.renderer = renderer;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int execute(int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, MAX_BATCH_SIZE));
        List<ReportJob> jobs = transactionTemplate.execute(status ->
            repository.claimQueuedForProcessing(limit, Instant.now(clock))
        );
        if (jobs == null || jobs.isEmpty()) {
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
            ReportDataset dataset = datasetProvider.load(job);
            RenderedReport renderedReport = renderer.render(job, dataset);
            Instant completedAt = Instant.now(clock);
            transactionTemplate.executeWithoutResult(status ->
                repository.markCompleted(
                        job.id(),
                        renderedReport.downloadUrl(),
                        renderedReport.storagePath(),
                        completedAt,
                        completedAt.plusSeconds(DEFAULT_EXPIRY_SECONDS))
            );
            log.info("[ACTION] Complete ReportExportJob | jobId={} | reportType={} | format={}",
                    LogMaskingUtil.maskId(job.id()),
                    job.reportType(),
                    job.format());
        } catch (RuntimeException exception) {
            Instant failedAt = Instant.now(clock);
            transactionTemplate.executeWithoutResult(status ->
                repository.markFailed(job.id(), failureReason(exception), failedAt)
            );
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
