package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.AdminResetPasswordCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.PasswordHashService;
import com.eprocure.iam.application.service.SessionService;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.PasswordHistoryRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminResetPasswordUseCase {
    private static final Logger log = LogManager.getLogger(AdminResetPasswordUseCase.class);
    private final UserRepository userRepository;
    private final PasswordHashService passwordHashService;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final SessionService sessionService;
    private final IdempotencyGuard idempotencyGuard;

    public AdminResetPasswordUseCase(
            UserRepository userRepository,
            PasswordHashService passwordHashService,
            PasswordHistoryRepository passwordHistoryRepository,
            SessionService sessionService,
            IdempotencyGuard idempotencyGuard) {
        this.userRepository = userRepository;
        this.passwordHashService = passwordHashService;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.sessionService = sessionService;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public void execute(AdminResetPasswordCommand command, String idempotencyKey) {
        idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start AdminResetPassword | actor={} | targetUserId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.targetUserId()));

        User user = userRepository.findById(command.targetUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));

        if (command.newPassword() == null || command.newPassword().isBlank()) {
            throw new BusinessException(ErrorCode.IAM_005);
        }

        String newPasswordHash = passwordHashService.hash(command.newPassword(), user.getId());
        userRepository.updatePasswordHash(user.getId(), newPasswordHash, command.actorId());
        
        Instant now = Instant.now();
        passwordHistoryRepository.save(user.getId(), newPasswordHash, command.actorId(), now);
        sessionService.revokeActiveForUser(user.getId(), command.actorId(), now);

        log.info("[ACTION] Complete AdminResetPassword | actor={} | targetUserId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(user.getId()));
    }
}
