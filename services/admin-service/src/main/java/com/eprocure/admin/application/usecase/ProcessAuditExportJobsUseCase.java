package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.port.out.AuditExportFileRenderer;
import com.eprocure.admin.application.port.out.RenderedAuditExport;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.domain.model.AuditExportJob;
import com.eprocure.admin.domain.model.AuditLogEntry;
import com.eprocure.admin.domain.model.AuditLogFilter;
import com.eprocure.admin.domain.repository.AuditExportJobRepository;
import com.eprocure.admin.domain.repository.AuditLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessAuditExportJobsUseCase {
    private static final Logger log = LogManager.getLogger(ProcessAuditExportJobsUseCase.class);
    private static final int MAX_BATCH_SIZE = 50;
    private static final long DEFAULT_EXPIRY_SECONDS = 604800;
    private static final int MAX_FAILURE_REASON_LENGTH = 500;

    private final AuditExportJobRepository exportJobRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditExportFileRenderer renderer;
    private final Clock clock;
    private final int maxRows;

    public ProcessAuditExportJobsUseCase(
            AuditExportJobRepository exportJobRepository,
            AuditLogRepository auditLogRepository,
            AuditExportFileRenderer renderer,
            Clock clock,
            @Value("${eprocure.admin.audit-export-worker.max-rows:5000}") int maxRows) {
        this.exportJobRepository = exportJobRepository;
        this.auditLogRepository = auditLogRepository;
        this.renderer = renderer;
        this.clock = clock;
        this.maxRows = Math.max(1, maxRows);
    }

    @Transactional
    public int execute(int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, MAX_BATCH_SIZE));
        List<AuditExportJob> jobs = exportJobRepository.claimQueuedForProcessing(limit, Instant.now(clock));
        if (jobs.isEmpty()) {
            return 0;
        }
        log.info("[ACTION] Start ProcessAuditExportJobs | count={}", jobs.size());
        for (AuditExportJob job : jobs) {
            processOne(job);
        }
        log.info("[ACTION] Complete ProcessAuditExportJobs | count={}", jobs.size());
        return jobs.size();
    }

    private void processOne(AuditExportJob job) {
        try {
            List<AuditLogEntry> entries = auditLogRepository.findForExport(toFilter(job), maxRows);
            RenderedAuditExport rendered = renderer.render(job, entries);
            Instant completedAt = Instant.now(clock);
            exportJobRepository.markCompleted(
                    job.id(),
                    rendered.fileName(),
                    rendered.storagePath(),
                    completedAt,
                    completedAt.plusSeconds(DEFAULT_EXPIRY_SECONDS));
            log.info("[ACTION] Complete AuditExportJob | jobId={} | rows={}",
                    LogMaskingUtil.maskId(job.id()),
                    entries.size());
        } catch (RuntimeException exception) {
            Instant failedAt = Instant.now(clock);
            exportJobRepository.markFailed(job.id(), failureReason(exception), failedAt);
            log.error("[EXCEPTION][ADM_AUDIT_EXPORT] Audit export job failed | jobId={} | error={}",
                    LogMaskingUtil.maskId(job.id()),
                    exception.getMessage(),
                    exception);
        }
    }

    private AuditLogFilter toFilter(AuditExportJob job) {
        return new AuditLogFilter(
                job.filterActorId().orElse(null),
                job.entityType().orElse(null),
                null,
                job.action().orElse(null),
                null,
                null,
                job.fromTime(),
                job.toTime(),
                1,
                maxRows,
                0);
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
