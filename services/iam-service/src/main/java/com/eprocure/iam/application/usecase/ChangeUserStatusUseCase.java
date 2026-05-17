package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.ChangeUserStatusCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.model.UserStatus;
import com.eprocure.iam.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeUserStatusUseCase {
    private static final Logger log = LoggerFactory.getLogger(ChangeUserStatusUseCase.class);
    private final UserRepository userRepository;
    private final IdempotencyGuard idempotencyGuard;

    public ChangeUserStatusUseCase(UserRepository userRepository, IdempotencyGuard idempotencyGuard) {
        this.userRepository = userRepository;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public void execute(ChangeUserStatusCommand command, String idempotencyKey) {
        idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start ChangeUserStatus | actor={} | userId={} | status={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.userId()),
                command.status());
        if (command.status() == UserStatus.LOCKED && (command.reason() == null || command.reason().isBlank())) {
            throw new BusinessException(ErrorCode.IAM_005);
        }
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));
        user.changeStatus(command.status());
        userRepository.updateStatus(user.getId(), user.getStatus(), command.actorId());
        log.info("[ACTION] Complete ChangeUserStatus | actor={} | userId={} | status={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(user.getId()),
                user.getStatus());
    }
}
