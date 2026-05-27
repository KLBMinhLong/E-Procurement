package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.ChangeMyPasswordCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.PasswordHashService;
import com.eprocure.iam.application.service.PasswordPolicyService;
import com.eprocure.iam.application.service.SessionService;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.PasswordHistoryRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeMyPasswordUseCase {
    private static final Logger log = LogManager.getLogger(ChangeMyPasswordUseCase.class);
    private final UserRepository userRepository;
    private final PasswordHashService passwordHashService;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final SessionService sessionService;
    private final PasswordPolicyService passwordPolicyService;
    private final IdempotencyGuard idempotencyGuard;

    public ChangeMyPasswordUseCase(
            UserRepository userRepository,
            PasswordHashService passwordHashService,
            PasswordHistoryRepository passwordHistoryRepository,
            SessionService sessionService,
            PasswordPolicyService passwordPolicyService,
            IdempotencyGuard idempotencyGuard) {
        this.userRepository = userRepository;
        this.passwordHashService = passwordHashService;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.sessionService = sessionService;
        this.passwordPolicyService = passwordPolicyService;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public void execute(ChangeMyPasswordCommand command, String idempotencyKey) {
        idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start ChangeMyPassword | userId={}", LogMaskingUtil.maskId(command.userId()));

        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));

        if (command.oldPassword() == null || command.oldPassword().isBlank()) {
            throw new BusinessException(ErrorCode.IAM_005);
        }

        if (command.newPassword() == null || command.newPassword().isBlank()) {
            throw new BusinessException(ErrorCode.IAM_005);
        }

        if (!command.newPassword().equals(command.confirmPassword())) {
            throw new BusinessException(ErrorCode.IAM_005);
        }

        // Verify current password hash matches old password
        String currentPasswordHash = userRepository.findPasswordHashById(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_001));

        if (!passwordHashService.matches(command.oldPassword(), user.getId(), currentPasswordHash)) {
            throw new BusinessException(ErrorCode.IAM_001); // Invalid credentials
        }

        // Validate using PasswordPolicyService for full password strength criteria (8+ chars, uppercase, lowercase, digit, special char, history check, identity check)
        List<String> recentHashes = new ArrayList<>(passwordHistoryRepository.findRecentHashesByUserId(user.getId(), 3));
        recentHashes.add(currentPasswordHash);
        passwordPolicyService.validate(user, command.newPassword(), command.confirmPassword(), recentHashes);

        String newPasswordHash = passwordHashService.hash(command.newPassword(), user.getId());
        userRepository.updatePasswordHash(user.getId(), newPasswordHash, command.userId());
        
        Instant now = Instant.now();
        passwordHistoryRepository.save(user.getId(), newPasswordHash, command.userId(), now);
        sessionService.revokeActiveForUser(user.getId(), command.userId(), now);

        log.info("[ACTION] Complete ChangeMyPassword | userId={}", LogMaskingUtil.maskId(user.getId()));
    }
}
