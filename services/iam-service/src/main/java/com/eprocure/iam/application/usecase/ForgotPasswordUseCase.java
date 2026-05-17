package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.ForgotPasswordCommand;
import com.eprocure.iam.application.port.out.PasswordResetDeliveryPort;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.OpaqueTokenService;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.PasswordResetToken;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.PasswordResetTokenRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ForgotPasswordUseCase {
    private static final Logger log = LogManager.getLogger(ForgotPasswordUseCase.class);
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetDeliveryPort passwordResetDeliveryPort;
    private final OpaqueTokenService opaqueTokenService;
    private final IdempotencyGuard idempotencyGuard;
    private final Duration resetTokenTtl;

    public ForgotPasswordUseCase(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordResetDeliveryPort passwordResetDeliveryPort,
            OpaqueTokenService opaqueTokenService,
            IdempotencyGuard idempotencyGuard,
            @Value("${eprocure.password-reset.ttl-minutes:15}") long resetTokenTtlMinutes) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordResetDeliveryPort = passwordResetDeliveryPort;
        this.opaqueTokenService = opaqueTokenService;
        this.idempotencyGuard = idempotencyGuard;
        this.resetTokenTtl = Duration.ofMinutes(resetTokenTtlMinutes);
    }

    @Transactional
    public void execute(ForgotPasswordCommand command) {
        idempotencyGuard.verify(command.idempotencyKey());
        log.info("[ACTION] Start ForgotPassword | email={}", LogMaskingUtil.maskEmail(command.email()));
        userRepository.findByUsernameOrEmail(command.email())
                .filter(User::canLogin)
                .ifPresentOrElse(
                        this::issueResetToken,
                        () -> log.info("[ACTION] Complete ForgotPassword | userId=anonymous"));
    }

    private void issueResetToken(User user) {
        Instant now = Instant.now();
        String rawToken = opaqueTokenService.generate();
        PasswordResetToken resetToken = PasswordResetToken.issue(
                UUID.randomUUID(),
                user.getId(),
                opaqueTokenService.hash(rawToken),
                now.plus(resetTokenTtl),
                now);
        passwordResetTokenRepository.revokeActiveByUserId(user.getId(), user.getId(), now);
        passwordResetTokenRepository.save(resetToken, user.getId());
        try {
            passwordResetDeliveryPort.sendResetInstructions(user, rawToken, resetToken.getExpiresAt());
        } catch (RuntimeException exception) {
            log.error("[EXCEPTION][SYS_002] Password reset delivery failed | userId={}",
                    LogMaskingUtil.maskId(user.getId()),
                    exception);
        }
        log.info("[ACTION] Complete ForgotPassword | userId={}", LogMaskingUtil.maskId(user.getId()));
    }
}
