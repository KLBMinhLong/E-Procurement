package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.service.KeycloakUserView;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.UserRepository;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetKeycloakUserUseCase {
    private static final Logger log = LogManager.getLogger(GetKeycloakUserUseCase.class);
    private final UserRepository userRepository;

    public GetKeycloakUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public KeycloakUserView byId(UUID userId) {
        log.info("[ACTION] Start GetKeycloakUserById | userId={}", LogMaskingUtil.maskId(userId));
        return userRepository.findById(userId)
                .map(this::toView)
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));
    }

    @Transactional(readOnly = true)
    public KeycloakUserView byLogin(String login) {
        log.info("[ACTION] Start GetKeycloakUserByLogin");
        return userRepository.findByUsernameOrEmail(login)
                .map(this::toView)
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));
    }

    private KeycloakUserView toView(User user) {
        return new KeycloakUserView(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.canLogin());
    }
}
