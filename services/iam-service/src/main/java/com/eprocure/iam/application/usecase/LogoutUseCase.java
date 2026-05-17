package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.service.SessionService;
import com.eprocure.iam.common.util.LogMaskingUtil;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutUseCase {
    private static final Logger log = LoggerFactory.getLogger(LogoutUseCase.class);
    private final SessionService sessionService;

    public LogoutUseCase(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Transactional
    public void execute(String rawToken, UUID actorId, UUID idempotencyKey) {
        log.info("[ACTION] Start Logout | userId={}", LogMaskingUtil.maskId(actorId));
        sessionService.revoke(rawToken, actorId);
        log.info("[ACTION] Complete Logout | userId={}", LogMaskingUtil.maskId(actorId));
    }
}
