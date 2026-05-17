package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.UpdateUserCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.UserDetailView;
import com.eprocure.iam.application.service.UserViewAssembler;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.DepartmentRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateUserUseCase {
    private static final Logger log = LoggerFactory.getLogger(UpdateUserUseCase.class);
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final UserViewAssembler userViewAssembler;
    private final IdempotencyGuard idempotencyGuard;

    public UpdateUserUseCase(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            UserViewAssembler userViewAssembler,
            IdempotencyGuard idempotencyGuard) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.userViewAssembler = userViewAssembler;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public UserDetailView execute(UpdateUserCommand command, String idempotencyKey) {
        idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start UpdateUser | actor={} | userId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.userId()));
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));
        UUID departmentId = command.departmentId() == null ? user.getDepartmentId() : command.departmentId();
        if (departmentRepository.findById(departmentId).isEmpty()) {
            throw new BusinessException(ErrorCode.IAM_033);
        }
        String fullName = command.fullName() == null ? user.getFullName() : command.fullName();
        if (fullName.isBlank()) {
            throw new BusinessException(ErrorCode.IAM_005);
        }
        user.updateProfile(
                fullName,
                command.phone(),
                departmentId,
                command.orgNodeId());
        userRepository.update(user, command.actorId());
        UserDetailView view = userRepository.findById(user.getId())
                .map(userViewAssembler::toDetail)
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));
        log.info("[ACTION] Complete UpdateUser | actor={} | userId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(user.getId()));
        return view;
    }
}
