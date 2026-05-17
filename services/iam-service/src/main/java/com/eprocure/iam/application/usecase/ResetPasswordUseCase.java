package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.ResetPasswordCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.OpaqueTokenService;
import com.eprocure.iam.application.service.PasswordHashService;
import com.eprocure.iam.application.service.PasswordPolicyService;
import com.eprocure.iam.application.service.SessionService;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.PasswordResetToken;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.PasswordHistoryRepository;
import com.eprocure.iam.domain.repository.PasswordResetTokenRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResetPasswordUseCase {
    private static final Logger log = LogManager.getLogger(ResetPasswordUseCase.class);
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final OpaqueTokenService opaqueTokenService;
    private final PasswordHashService passwordHashService;
    private final PasswordPolicyService passwordPolicyService;
    private final IdempotencyGuard idempotencyGuard;

    public ResetPasswordUseCase(
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordHistoryRepository passwordHistoryRepository,
            UserRepository userRepository,
            SessionService sessionService,
            OpaqueTokenService opaqueTokenService,
            PasswordHashService passwordHashService,
            PasswordPolicyService passwordPolicyService,
            IdempotencyGuard idempotencyGuard) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.userRepository = userRepository;
        this.sessionService = sessionService;
        this.opaqueTokenService = opaqueTokenService;
        this.passwordHashService = passwordHashService;
        this.passwordPolicyService = passwordPolicyService;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public void execute(ResetPasswordCommand command) {
        idempotencyGuard.verify(command.idempotencyKey());
        log.info("[ACTION] Start ResetPassword | tokenHash={}", maskedTokenHash(command.resetToken()));
        Instant now = Instant.now();
        PasswordResetToken resetToken = passwordResetTokenRepository.findActiveByTokenHash(tokenHash(command.resetToken()), now)
                .filter(token -> token.isActive(now))
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_007));
        User user = userRepository.findById(resetToken.getUserId())
                .filter(User::canLogin)
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_007));
        List<String> recentHashes = new ArrayList<>(passwordHistoryRepository.findRecentHashesByUserId(user.getId(), 3));
        userRepository.findPasswordHashById(user.getId()).ifPresent(recentHashes::add);
        passwordPolicyService.validate(user, command.newPassword(), command.confirmPassword(), recentHashes);

        String newPasswordHash = passwordHashService.hash(command.newPassword(), user.getId());
        userRepository.updatePasswordHash(user.getId(), newPasswordHash, user.getId());
        passwordHistoryRepository.save(user.getId(), newPasswordHash, user.getId(), now);
        passwordResetTokenRepository.markUsed(resetToken.getId(), user.getId(), now);
        passwordResetTokenRepository.revokeActiveByUserId(user.getId(), user.getId(), now);
        sessionService.revokeActiveForUser(user.getId(), user.getId(), now);
        log.info("[ACTION] Complete ResetPassword | userId={}", LogMaskingUtil.maskId(user.getId()));
    }

    private String tokenHash(String rawToken) {
        if (!rawToken.matches("^[a-f0-9]{64}$")) {
            throw new BusinessException(ErrorCode.IAM_007);
        }
        return opaqueTokenService.hash(rawToken);
    }

    private String maskedTokenHash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return "***";
        }
        return opaqueTokenService.hash(rawToken).substring(0, 8) + "...";
    }
}
