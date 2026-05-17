package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.VerifyTwoFactorCommand;
import com.eprocure.iam.application.service.CreatedSession;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.SessionService;
import com.eprocure.iam.application.service.TotpSecretCipher;
import com.eprocure.iam.application.service.TotpService;
import com.eprocure.iam.application.service.TwoFactorChallengeData;
import com.eprocure.iam.application.service.TwoFactorChallengeService;
import com.eprocure.iam.application.service.TwoFactorVerificationResult;
import com.eprocure.iam.application.service.UserViewAssembler;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Instant;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerifyTwoFactorUseCase {
    private static final Logger log = LogManager.getLogger(VerifyTwoFactorUseCase.class);

    private final UserRepository userRepository;
    private final IdempotencyGuard idempotencyGuard;
    private final TotpService totpService;
    private final TotpSecretCipher totpSecretCipher;
    private final TwoFactorChallengeService challengeService;
    private final SessionService sessionService;
    private final UserViewAssembler userViewAssembler;

    public VerifyTwoFactorUseCase(
            UserRepository userRepository,
            IdempotencyGuard idempotencyGuard,
            TotpService totpService,
            TotpSecretCipher totpSecretCipher,
            TwoFactorChallengeService challengeService,
            SessionService sessionService,
            UserViewAssembler userViewAssembler) {
        this.userRepository = userRepository;
        this.idempotencyGuard = idempotencyGuard;
        this.totpService = totpService;
        this.totpSecretCipher = totpSecretCipher;
        this.challengeService = challengeService;
        this.sessionService = sessionService;
        this.userViewAssembler = userViewAssembler;
    }

    @Transactional
    public TwoFactorVerificationResult execute(VerifyTwoFactorCommand command) {
        idempotencyGuard.verify(command.idempotencyKey());
        log.info("[ACTION] Start VerifyTwoFactor");
        TwoFactorChallengeData challenge = challengeService.findByRawToken(command.challengeToken())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_006));
        User user = userRepository.findById(challenge.userId())
                .filter(User::canLogin)
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_003));
        String encryptedSecret = user.getTwoFactorSecretEncrypted()
                .filter(secret -> user.isTwoFactorEnabled())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_006));
        if (!totpService.verifyCode(totpSecretCipher.decrypt(encryptedSecret), command.code())) {
            throw new BusinessException(ErrorCode.IAM_006);
        }
        CreatedSession createdSession = sessionService.issueFor(user, command.clientContext());
        userRepository.updateLastLoginAt(user.getId(), Instant.now());
        challengeService.evict(command.challengeToken());
        log.info("[ACTION] Complete VerifyTwoFactor | userId={}", LogMaskingUtil.maskId(user.getId()));
        return new TwoFactorVerificationResult(userViewAssembler.toSummary(user), createdSession.rawToken());
    }
}
