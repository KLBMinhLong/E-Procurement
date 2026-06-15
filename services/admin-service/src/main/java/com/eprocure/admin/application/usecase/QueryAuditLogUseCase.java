package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.service.PageResult;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.domain.model.AuditLogEntry;
import com.eprocure.admin.domain.model.AuditLogFilter;
import com.eprocure.admin.domain.repository.AuditLogRepository;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueryAuditLogUseCase {
    private static final Logger log = LogManager.getLogger(QueryAuditLogUseCase.class);

    private final AuditLogRepository repository;

    public QueryAuditLogUseCase(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResult<AuditLogEntry> execute(AuditLogFilter filter, UUID actorId) {
        validate(filter);
        log.info("[ACTION] Start QueryAuditLog | userId={} | from={} | to={} | page={} | size={}",
                LogMaskingUtil.maskId(actorId),
                filter.fromTime(),
                filter.toTime(),
                filter.page(),
                filter.size());
        PageResult<AuditLogEntry> result = repository.findByFilter(filter);
        log.info("[ACTION] Complete QueryAuditLog | userId={} | total={}",
                LogMaskingUtil.maskId(actorId),
                result.meta().totalElements());
        return result;
    }

    private void validate(AuditLogFilter filter) {
        if (filter.fromTime() == null || filter.toTime() == null || filter.fromTime().isAfter(filter.toTime())) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
    }
}
