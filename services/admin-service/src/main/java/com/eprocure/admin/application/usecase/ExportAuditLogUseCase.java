package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.port.in.ExportAuditLogCommand;
import com.eprocure.admin.application.service.AuditExportJobMutationResult;
import com.eprocure.admin.application.service.IdempotencyGuard;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.domain.model.AuditExportJob;
import com.eprocure.admin.domain.repository.AuditExportJobRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportAuditLogUseCase {
    private static final Logger log = LogManager.getLogger(ExportAuditLogUseCase.class);

    private final AuditExportJobRepository repository;
    private final IdempotencyGuard idempotencyGuard;
    private final Clock clock;

    public ExportAuditLogUseCase(
            AuditExportJobRepository repository,
            IdempotencyGuard idempotencyGuard,
            Clock clock) {
        this.repository = repository;
        this.idempotencyGuard = idempotencyGuard;
        this.clock = clock;
    }

    @Transactional
    public AuditExportJobMutationResult execute(ExportAuditLogCommand command, String idempotencyKey) {
        UUID key = idempotencyGuard.verify(idempotencyKey);
        return repository.findByIdempotencyKey(command.requestedBy(), key)
                .map(job -> {
                    log.info("[ACTION] Replay ExportAuditLog | userId={} | jobId={}",
                            LogMaskingUtil.maskId(command.requestedBy()),
                            LogMaskingUtil.maskId(job.id()));
                    return new AuditExportJobMutationResult(job, true);
                })
                .orElseGet(() -> createJob(command, key));
    }

    private AuditExportJobMutationResult createJob(ExportAuditLogCommand command, UUID idempotencyKey) {
        validate(command);
        Instant now = Instant.now(clock);
        AuditExportJob job = AuditExportJob.queued(
                UUID.randomUUID(),
                command.fromTime(),
                command.toTime(),
                command.filterActorId(),
                command.entityType(),
                command.action(),
                idempotencyKey,
                now,
                now.plus(7, ChronoUnit.DAYS),
                command.requestedBy());
        AuditExportJob saved = repository.saveQueued(job);
        log.info("[ACTION] Complete ExportAuditLog | userId={} | jobId={} | from={} | to={}",
                LogMaskingUtil.maskId(command.requestedBy()),
                LogMaskingUtil.maskId(saved.id()),
                saved.fromTime(),
                saved.toTime());
        return new AuditExportJobMutationResult(saved, false);
    }

    private void validate(ExportAuditLogCommand command) {
        if (command.fromTime() == null || command.toTime() == null || command.fromTime().isAfter(command.toTime())) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
    }
}
