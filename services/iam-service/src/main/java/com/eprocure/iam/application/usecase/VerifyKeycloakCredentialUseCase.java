package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.service.PasswordHashService;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerifyKeycloakCredentialUseCase {
    private static final Logger log = LogManager.getLogger(VerifyKeycloakCredentialUseCase.class);
    private final UserRepository userRepository;
    private final PasswordHashService passwordHashService;

    public VerifyKeycloakCredentialUseCase(UserRepository userRepository, PasswordHashService passwordHashService) {
        this.userRepository = userRepository;
        this.passwordHashService = passwordHashService;
    }

    @Transactional(readOnly = true)
    public boolean execute(String username, String password) {
        log.info("[ACTION] Start VerifyKeycloakCredential | username={}", LogMaskingUtil.maskEmail(username));
        return userRepository.findByUsernameOrEmail(username)
                .filter(User::canLogin)
                .flatMap(user -> userRepository.findPasswordHashById(user.getId())
                        .map(passwordHash -> passwordHashService.matches(password, user.getId(), passwordHash)))
                .orElse(false);
    }
}
