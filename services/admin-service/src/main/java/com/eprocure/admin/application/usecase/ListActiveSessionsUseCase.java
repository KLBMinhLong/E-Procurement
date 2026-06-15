package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.port.in.ListActiveSessionsQuery;
import com.eprocure.admin.application.port.out.IamSessionAdminPort;
import com.eprocure.admin.application.service.ActiveSessionView;
import com.eprocure.admin.application.service.PageResult;
import com.eprocure.admin.common.util.LogMaskingUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListActiveSessionsUseCase {
    private static final Logger log = LogManager.getLogger(ListActiveSessionsUseCase.class);

    private final IamSessionAdminPort iamSessionAdminPort;

    public ListActiveSessionsUseCase(IamSessionAdminPort iamSessionAdminPort) {
        this.iamSessionAdminPort = iamSessionAdminPort;
    }

    @Transactional(readOnly = true)
    public PageResult<ActiveSessionView> execute(ListActiveSessionsQuery query) {
        log.info("[ACTION] Start ListActiveSessions | filterUserId={} | page={} | size={}",
                LogMaskingUtil.maskId(query.userId()),
                query.page(),
                query.size());
        PageResult<ActiveSessionView> result = iamSessionAdminPort.listActiveSessions(
                query.userId(),
                query.page(),
                query.size());
        log.info("[ACTION] Complete ListActiveSessions | total={}", result.meta().totalElements());
        return result;
    }
}
