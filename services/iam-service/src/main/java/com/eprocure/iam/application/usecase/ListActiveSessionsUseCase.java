package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.ListActiveSessionsQuery;
import com.eprocure.iam.application.service.ActiveSessionView;
import com.eprocure.iam.application.service.PageMeta;
import com.eprocure.iam.application.service.PageResult;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.ActiveSession;
import com.eprocure.iam.domain.repository.Page;
import com.eprocure.iam.domain.repository.SessionRepository;
import java.time.Clock;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListActiveSessionsUseCase {
    private static final Logger log = LogManager.getLogger(ListActiveSessionsUseCase.class);
    private static final String DEFAULT_SORT = "issuedAt,desc";

    private final SessionRepository sessionRepository;
    private final Clock clock;

    public ListActiveSessionsUseCase(SessionRepository sessionRepository, Clock clock) {
        this.sessionRepository = sessionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResult<ActiveSessionView> execute(ListActiveSessionsQuery query) {
        log.info("[ACTION] Start ListActiveSessions | filterUserId={} | page={} | size={}",
                LogMaskingUtil.maskId(query.userId()),
                query.page(),
                query.size());
        Page<ActiveSession> page = sessionRepository.findActivePage(
                query.userId(),
                query.offset(),
                query.size(),
                Instant.now(clock));
        log.info("[ACTION] Complete ListActiveSessions | total={}", page.totalElements());
        return new PageResult<>(
                page.items().stream().map(this::toView).toList(),
                PageMeta.of(page.totalElements(), query.page(), query.size(), DEFAULT_SORT));
    }

    private ActiveSessionView toView(ActiveSession session) {
        return new ActiveSessionView(
                session.sessionId(),
                session.userId(),
                session.username(),
                session.fullName(),
                session.ipAddress().orElse(null),
                session.userAgent().orElse(null),
                session.issuedAt(),
                session.lastActivityAt(),
                session.expiresAt());
    }
}
