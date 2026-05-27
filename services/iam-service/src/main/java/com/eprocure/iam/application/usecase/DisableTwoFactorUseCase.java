package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.DisableTwoFactorCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.UserRepository;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DisableTwoFactorUseCase {
    private static final Logger log = LogManager.getLogger(DisableTwoFactorUseCase.class);

    private final UserRepository userRepository;
    private final IdempotencyGuard idempotencyGuard;

    public DisableTwoFactorUseCase(UserRepository userRepository, IdempotencyGuard idempotencyGuard) {
        this.userRepository = userRepository;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public void execute(DisableTwoFactorCommand command, String idempotencyKey) {
        idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start DisableTwoFactor | userId={}", LogMaskingUtil.maskId(command.userId()));
        User user = userRepository.findById(command.userId())
                .filter(User::canLogin)
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));
        
        userRepository.disableTwoFactor(user.getId(), user.getId());
        log.info("[ACTION] Complete DisableTwoFactor | userId={}", LogMaskingUtil.maskId(user.getId()));
    }
}
