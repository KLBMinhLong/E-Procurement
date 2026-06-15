package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.VerifyUserTotpCommand;
import com.eprocure.iam.application.service.TotpSecretCipher;
import com.eprocure.iam.application.service.TotpService;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerifyUserTotpUseCase {
    private static final Logger log = LogManager.getLogger(VerifyUserTotpUseCase.class);

    private final UserRepository userRepository;
    private final TotpService totpService;
    private final TotpSecretCipher totpSecretCipher;

    public VerifyUserTotpUseCase(
            UserRepository userRepository,
            TotpService totpService,
            TotpSecretCipher totpSecretCipher) {
        this.userRepository = userRepository;
        this.totpService = totpService;
        this.totpSecretCipher = totpSecretCipher;
    }

    @Transactional(readOnly = true)
    public void execute(VerifyUserTotpCommand command) {
        log.info("[ACTION] Start VerifyUserTotp | userId={}", LogMaskingUtil.maskId(command.userId()));
        User user = userRepository.findById(command.userId())
                .filter(User::canLogin)
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));
        String encryptedSecret = user.getTwoFactorSecretEncrypted()
                .filter(secret -> user.isTwoFactorEnabled())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_006));
        if (!totpService.verifyCode(totpSecretCipher.decrypt(encryptedSecret), command.code())) {
            throw new BusinessException(ErrorCode.IAM_006);
        }
        log.info("[ACTION] Complete VerifyUserTotp | userId={}", LogMaskingUtil.maskId(command.userId()));
    }
}
