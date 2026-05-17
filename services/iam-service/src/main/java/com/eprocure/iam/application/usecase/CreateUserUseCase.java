package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.CreateUserCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.UserDetailView;
import com.eprocure.iam.application.service.UserViewAssembler;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.model.UserStatus;
import com.eprocure.iam.domain.repository.DepartmentRepository;
import com.eprocure.iam.domain.repository.RoleRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateUserUseCase {
    private static final Logger log = LoggerFactory.getLogger(CreateUserUseCase.class);
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final UserViewAssembler userViewAssembler;
    private final IdempotencyGuard idempotencyGuard;

    public CreateUserUseCase(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            RoleRepository roleRepository,
            UserViewAssembler userViewAssembler,
            IdempotencyGuard idempotencyGuard) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.userViewAssembler = userViewAssembler;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public UserDetailView execute(CreateUserCommand command, String idempotencyKey) {
        idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start CreateUser | actor={} | username={}",
                LogMaskingUtil.maskId(command.actorId()),
                command.username());
        userRepository.findByEmployeeCodeOrUsernameOrEmail(command.employeeCode(), command.username(), command.email())
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.IAM_009);
                });
        if (departmentRepository.findById(command.departmentId()).isEmpty()) {
            throw new BusinessException(ErrorCode.IAM_033);
        }
        Set<String> roleCodes = CodeSetUtil.normalize(command.roles());
        validateRoles(roleCodes);

        User user = User.create(
                UUID.randomUUID(),
                command.employeeCode(),
                command.username(),
                command.email(),
                command.fullName(),
                command.phone(),
                command.departmentId(),
                command.orgNodeId(),
                UserStatus.PENDING_VERIFY,
                false,
                Instant.now());
        userRepository.save(user, command.actorId());
        userRepository.replaceRoles(user.getId(), roleCodes, command.actorId());
        UserDetailView view = userRepository.findById(user.getId())
                .map(userViewAssembler::toDetail)
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));
        log.info("[ACTION] Complete CreateUser | actor={} | userId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(user.getId()));
        return view;
    }

    private void validateRoles(Set<String> roleCodes) {
        if (!roleRepository.findExistingCodes(roleCodes).containsAll(roleCodes)) {
            throw new BusinessException(ErrorCode.IAM_031);
        }
    }
}
