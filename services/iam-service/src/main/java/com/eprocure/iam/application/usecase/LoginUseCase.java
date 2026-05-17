package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.LoginCommand;
import com.eprocure.iam.application.port.out.CredentialVerificationPort;
import com.eprocure.iam.application.service.CreatedSession;
import com.eprocure.iam.application.service.LoginResult;
import com.eprocure.iam.application.service.SessionService;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginUseCase {
    private static final Logger log = LoggerFactory.getLogger(LoginUseCase.class);
    private final UserRepository userRepository;
    private final CredentialVerificationPort credentialVerificationPort;
    private final SessionService sessionService;

    public LoginUseCase(
            UserRepository userRepository,
            CredentialVerificationPort credentialVerificationPort,
            SessionService sessionService) {
        this.userRepository = userRepository;
        this.credentialVerificationPort = credentialVerificationPort;
        this.sessionService = sessionService;
    }

    @Transactional
    public LoginResult execute(LoginCommand command) {
        log.info("[ACTION] Start Login | username={}", LogMaskingUtil.maskEmail(command.username()));
        User user = userRepository.findByUsernameOrEmail(command.username())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_001));
        if (user.isLocked() || !user.canLogin()) {
            throw new BusinessException(ErrorCode.IAM_002);
        }
        if (!credentialVerificationPort.verify(command.username(), command.password())) {
            throw new BusinessException(ErrorCode.IAM_001);
        }

        CreatedSession createdSession = sessionService.issueFor(user, command.clientContext());
        userRepository.updateLastLoginAt(user.getId(), Instant.now());
        log.info("[ACTION] Complete Login | userId={}", LogMaskingUtil.maskId(user.getId()));
        return new LoginResult(
                user.getId(),
                user.getFullName(),
                user.getAvatarUrl().orElse(null),
                user.isTwoFactorEnabled(),
                createdSession.rawToken());
    }
}
