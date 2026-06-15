package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.InvalidateSessionCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.SessionService;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import java.time.Clock;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvalidateSessionUseCase {
    private static final Logger log = LogManager.getLogger(InvalidateSessionUseCase.class);

    private final SessionService sessionService;
    private final IdempotencyGuard idempotencyGuard;
    private final Clock clock;

    public InvalidateSessionUseCase(SessionService sessionService, IdempotencyGuard idempotencyGuard, Clock clock) {
        this.sessionService = sessionService;
        this.idempotencyGuard = idempotencyGuard;
        this.clock = clock;
    }

    @Transactional
    public void execute(InvalidateSessionCommand command, String idempotencyKey) {
        idempotencyGuard.verify(idempotencyKey);
        if (command.reason().isBlank()) {
            throw new BusinessException(ErrorCode.IAM_005);
        }
        log.info("[ACTION] Start InvalidateSession | sessionId={} | actorId={}",
                LogMaskingUtil.maskId(command.sessionId()),
                LogMaskingUtil.maskId(command.actorId()));
        sessionService.revokeActiveSession(command.sessionId(), command.actorId(), Instant.now(clock));
        log.info("[ACTION] Complete InvalidateSession | sessionId={} | actorId={}",
                LogMaskingUtil.maskId(command.sessionId()),
                LogMaskingUtil.maskId(command.actorId()));
    }
}
