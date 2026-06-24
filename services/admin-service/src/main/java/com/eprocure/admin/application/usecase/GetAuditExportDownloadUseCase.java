package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.port.in.GetAuditExportJobQuery;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.domain.model.AuditExportJob;
import com.eprocure.admin.domain.model.AuditExportJobStatus;
import com.eprocure.admin.domain.repository.AuditExportJobRepository;
import java.time.Clock;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAuditExportDownloadUseCase {
    private static final Logger log = LogManager.getLogger(GetAuditExportDownloadUseCase.class);

    private final AuditExportJobRepository repository;
    private final Clock clock;

    public GetAuditExportDownloadUseCase(AuditExportJobRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AuditExportJob execute(GetAuditExportJobQuery query) {
        log.info("[ACTION] Start GetAuditExportDownload | userId={} | jobId={}",
                LogMaskingUtil.maskId(query.actorId()),
                LogMaskingUtil.maskId(query.jobId()));
        AuditExportJob job = repository.findByIdAndActorId(query.jobId(), query.actorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDIT_EXPORT_JOB_NOT_FOUND));
        if (job.status() != AuditExportJobStatus.COMPLETED
                || job.fileName().isEmpty()
                || job.storagePath().isEmpty()
                || isExpired(job)) {
            throw new BusinessException(ErrorCode.AUDIT_EXPORT_FILE_NOT_READY);
        }
        log.info("[ACTION] Complete GetAuditExportDownload | userId={} | jobId={}",
                LogMaskingUtil.maskId(query.actorId()),
                LogMaskingUtil.maskId(query.jobId()));
        return job;
    }

    private boolean isExpired(AuditExportJob job) {
        return job.expiresAt()
                .map(expiresAt -> expiresAt.isBefore(Instant.now(clock)))
                .orElse(false);
    }
}
