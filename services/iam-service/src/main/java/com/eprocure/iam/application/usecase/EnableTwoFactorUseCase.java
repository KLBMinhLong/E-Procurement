package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.EnableTwoFactorCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.TotpSecretCipher;
import com.eprocure.iam.application.service.TotpService;
import com.eprocure.iam.application.service.TwoFactorSetupView;
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
public class EnableTwoFactorUseCase {
    private static final Logger log = LogManager.getLogger(EnableTwoFactorUseCase.class);

    private final UserRepository userRepository;
    private final IdempotencyGuard idempotencyGuard;
    private final TotpService totpService;
    private final TotpSecretCipher totpSecretCipher;

    public EnableTwoFactorUseCase(
            UserRepository userRepository,
            IdempotencyGuard idempotencyGuard,
            TotpService totpService,
            TotpSecretCipher totpSecretCipher) {
        this.userRepository = userRepository;
        this.idempotencyGuard = idempotencyGuard;
        this.totpService = totpService;
        this.totpSecretCipher = totpSecretCipher;
    }

    @Transactional
    public TwoFactorSetupView execute(EnableTwoFactorCommand command, String idempotencyKey) {
        idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start EnableTwoFactor | userId={}", LogMaskingUtil.maskId(command.userId()));
        User user = userRepository.findById(command.userId())
                .filter(User::canLogin)
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));
        String secret = totpService.generateSecret();
        userRepository.stageTwoFactorSecret(user.getId(), totpSecretCipher.encrypt(secret), user.getId());
        log.info("[ACTION] Complete EnableTwoFactor | userId={}", LogMaskingUtil.maskId(user.getId()));
        return new TwoFactorSetupView(secret, totpService.provisioningUri(user.getUsername(), secret), secret);
    }
}
