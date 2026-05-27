package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.UpdateMyProfileCommand;
import com.eprocure.iam.application.service.CurrentUserView;
import com.eprocure.iam.application.service.IdempotencyGuard;
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
public class UpdateMyProfileUseCase {
    private static final Logger log = LogManager.getLogger(UpdateMyProfileUseCase.class);
    private final UserRepository userRepository;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final IdempotencyGuard idempotencyGuard;

    public UpdateMyProfileUseCase(
            UserRepository userRepository,
            GetCurrentUserUseCase getCurrentUserUseCase,
            IdempotencyGuard idempotencyGuard) {
        this.userRepository = userRepository;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public CurrentUserView execute(UpdateMyProfileCommand command, String idempotencyKey) {
        idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start UpdateMyProfile | userId={}", LogMaskingUtil.maskId(command.userId()));
        
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));
        
        String fullName = command.fullName() == null ? user.getFullName() : command.fullName();
        if (fullName.isBlank()) {
            throw new BusinessException(ErrorCode.IAM_005);
        }
        
        user.updateMyProfile(fullName, command.phone(), command.avatarUrl());
        userRepository.update(user, command.userId());
        
        CurrentUserView view = getCurrentUserUseCase.execute(user.getId());
        
        log.info("[ACTION] Complete UpdateMyProfile | userId={}", LogMaskingUtil.maskId(user.getId()));
        return view;
    }
}
