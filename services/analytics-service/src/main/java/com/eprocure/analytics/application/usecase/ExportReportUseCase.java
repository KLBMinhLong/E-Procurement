package com.eprocure.analytics.application.usecase;

import com.eprocure.analytics.application.port.in.ExportReportCommand;
import com.eprocure.analytics.application.service.ReportJobMutationResult;
import com.eprocure.analytics.common.exception.BusinessException;
import com.eprocure.analytics.common.exception.ErrorCode;
import com.eprocure.analytics.common.util.LogMaskingUtil;
import com.eprocure.analytics.domain.model.report.ReportJob;
import com.eprocure.analytics.domain.repository.ReportJobRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportReportUseCase {
    private static final Logger log = LogManager.getLogger(ExportReportUseCase.class);

    private final ReportJobRepository repository;
    private final Clock clock;

    public ExportReportUseCase(ReportJobRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public ReportJobMutationResult execute(ExportReportCommand command, String idempotencyKey) {
        UUID key = requireIdempotencyKey(idempotencyKey);
        return repository.findByIdempotencyKey(command.actorId(), key)
                .map(job -> {
                    log.info("[ACTION] Replay ExportReport | userId={} | jobId={}",
                            LogMaskingUtil.maskId(command.actorId()),
                            LogMaskingUtil.maskId(job.id()));
                    return new ReportJobMutationResult(job, true);
                })
                .orElseGet(() -> createJob(command, key));
    }

    private ReportJobMutationResult createJob(ExportReportCommand command, UUID idempotencyKey) {
        Instant now = Instant.now(clock);
        ReportJob job = ReportJob.queued(
                UUID.randomUUID(),
                command.reportType(),
                command.format(),
                now,
                now.plus(7, ChronoUnit.DAYS),
                command.actorId(),
                idempotencyKey);
        repository.saveQueued(job, command.filters());
        log.info("[ACTION] Complete ExportReport | userId={} | jobId={} | reportType={} | format={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(job.id()),
                job.reportType(),
                job.format());
        return new ReportJobMutationResult(job, false);
    }

    private UUID requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.SYS_005);
        }
        try {
            UUID key = UUID.fromString(idempotencyKey);
            if (key.version() != 4 || !idempotencyKey.equals(idempotencyKey.toLowerCase(Locale.ROOT))) {
                throw new BusinessException(ErrorCode.SYS_005);
            }
            return key;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.SYS_005);
        }
    }
}
