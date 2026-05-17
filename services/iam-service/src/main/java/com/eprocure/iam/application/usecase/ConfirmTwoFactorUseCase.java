package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.ConfirmTwoFactorCommand;
import com.eprocure.iam.application.service.BackupCodeSet;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.TotpSecretCipher;
import com.eprocure.iam.application.service.TotpService;
import com.eprocure.iam.application.service.TwoFactorConfirmView;
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
public class ConfirmTwoFactorUseCase {
    private static final Logger log = LogManager.getLogger(ConfirmTwoFactorUseCase.class);

    private final UserRepository userRepository;
    private final IdempotencyGuard idempotencyGuard;
    private final TotpService totpService;
    private final TotpSecretCipher totpSecretCipher;

    public ConfirmTwoFactorUseCase(
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
    public TwoFactorConfirmView execute(ConfirmTwoFactorCommand command, String idempotencyKey) {
        idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start ConfirmTwoFactor | userId={}", LogMaskingUtil.maskId(command.userId()));
        User user = userRepository.findById(command.userId())
                .filter(User::canLogin)
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));
        String encryptedSecret = user.getTwoFactorPendingSecretEncrypted()
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_006));
        String secret = totpSecretCipher.decrypt(encryptedSecret);
        if (!totpService.verifyCode(secret, command.code())) {
            throw new BusinessException(ErrorCode.IAM_006);
        }
        BackupCodeSet backupCodes = totpService.generateBackupCodes();
        userRepository.confirmTwoFactor(
                user.getId(),
                encryptedSecret,
                backupCodes.codeHashes(),
                Instant.now(),
                user.getId());
        log.info("[ACTION] Complete ConfirmTwoFactor | userId={}", LogMaskingUtil.maskId(user.getId()));
        return new TwoFactorConfirmView(backupCodes.rawCodes());
    }
}
