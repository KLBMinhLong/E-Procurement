package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.port.in.InvalidateSessionCommand;
import com.eprocure.admin.application.port.out.IamSessionAdminPort;
import com.eprocure.admin.application.service.IdempotencyGuard;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import com.eprocure.admin.common.util.LogMaskingUtil;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvalidateSessionUseCase {
    private static final Logger log = LogManager.getLogger(InvalidateSessionUseCase.class);

    private final IamSessionAdminPort iamSessionAdminPort;
    private final IdempotencyGuard idempotencyGuard;

    public InvalidateSessionUseCase(IamSessionAdminPort iamSessionAdminPort, IdempotencyGuard idempotencyGuard) {
        this.iamSessionAdminPort = iamSessionAdminPort;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public void execute(InvalidateSessionCommand command, String idempotencyKey) {
        UUID key = idempotencyGuard.verify(idempotencyKey);
        if (command.reason().isBlank()) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
        log.info("[ACTION] Start InvalidateSession | userId={} | sessionId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.sessionId()));
        iamSessionAdminPort.invalidateSession(command.sessionId(), command.actorId(), command.reason(), key);
        log.info("[ACTION] Complete InvalidateSession | userId={} | sessionId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.sessionId()));
    }
}
