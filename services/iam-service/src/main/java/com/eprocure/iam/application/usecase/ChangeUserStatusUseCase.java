package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.ChangeUserStatusCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.SessionService;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.model.UserStatus;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Instant;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeUserStatusUseCase {
    private static final Logger log = LogManager.getLogger(ChangeUserStatusUseCase.class);
    private final UserRepository userRepository;
    private final IdempotencyGuard idempotencyGuard;
    private final SessionService sessionService;

    public ChangeUserStatusUseCase(UserRepository userRepository, IdempotencyGuard idempotencyGuard, SessionService sessionService) {
        this.userRepository = userRepository;
        this.idempotencyGuard = idempotencyGuard;
        this.sessionService = sessionService;
    }

    @Transactional
    public void execute(ChangeUserStatusCommand command, String idempotencyKey) {
        idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start ChangeUserStatus | actor={} | userId={} | status={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.userId()),
                command.status());

        // Prevent admin from locking/deactivating their own account
        if (command.actorId().equals(command.userId())) {
            throw new BusinessException(ErrorCode.IAM_036);
        }

        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));
        user.changeStatus(command.status());
        userRepository.updateStatus(user.getId(), user.getStatus(), command.actorId());

        // When locking or deactivating, revoke all active sessions immediately (kick user out)
        if (command.status() == UserStatus.LOCKED || command.status() == UserStatus.INACTIVE) {
            sessionService.revokeActiveForUser(user.getId(), command.actorId(), Instant.now());
            log.info("[ACTION] Step RevokeSessionsAfterLock | userId={}", LogMaskingUtil.maskId(user.getId()));
        }

        log.info("[ACTION] Complete ChangeUserStatus | actor={} | userId={} | status={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(user.getId()),
                user.getStatus());
    }
}
