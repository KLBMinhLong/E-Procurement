package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.service.UserDetailView;
import com.eprocure.iam.application.service.UserViewAssembler;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.repository.UserRepository;
import java.util.UUID;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserByIdUseCase {
    private static final Logger log = LogManager.getLogger(GetUserByIdUseCase.class);
    private final UserRepository userRepository;
    private final UserViewAssembler userViewAssembler;

    public GetUserByIdUseCase(UserRepository userRepository, UserViewAssembler userViewAssembler) {
        this.userRepository = userRepository;
        this.userViewAssembler = userViewAssembler;
    }

    @Transactional(readOnly = true)
    public UserDetailView execute(UUID userId) {
        log.info("[ACTION] Start GetUserById | userId={}", LogMaskingUtil.maskId(userId));
        UserDetailView view = userRepository.findById(userId)
                .map(userViewAssembler::toDetail)
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));
        log.info("[ACTION] Complete GetUserById | userId={}", LogMaskingUtil.maskId(userId));
        return view;
    }
}
