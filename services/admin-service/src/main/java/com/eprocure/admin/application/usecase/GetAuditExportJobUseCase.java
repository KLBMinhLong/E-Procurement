package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.port.in.GetAuditExportJobQuery;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.domain.model.AuditExportJob;
import com.eprocure.admin.domain.repository.AuditExportJobRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAuditExportJobUseCase {
    private static final Logger log = LogManager.getLogger(GetAuditExportJobUseCase.class);

    private final AuditExportJobRepository repository;

    public GetAuditExportJobUseCase(AuditExportJobRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AuditExportJob execute(GetAuditExportJobQuery query) {
        log.info("[ACTION] Start GetAuditExportJob | userId={} | jobId={}",
                LogMaskingUtil.maskId(query.actorId()),
                LogMaskingUtil.maskId(query.jobId()));
        AuditExportJob job = repository.findByIdAndActorId(query.jobId(), query.actorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDIT_EXPORT_JOB_NOT_FOUND));
        log.info("[ACTION] Complete GetAuditExportJob | userId={} | jobId={} | status={}",
                LogMaskingUtil.maskId(query.actorId()),
                LogMaskingUtil.maskId(query.jobId()),
                job.status());
        return job;
    }
}
