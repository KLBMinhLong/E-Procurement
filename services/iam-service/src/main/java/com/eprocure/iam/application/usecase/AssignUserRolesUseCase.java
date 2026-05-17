package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.AssignUserRolesCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.repository.RoleRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignUserRolesUseCase {
    private static final Logger log = LoggerFactory.getLogger(AssignUserRolesUseCase.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final IdempotencyGuard idempotencyGuard;

    public AssignUserRolesUseCase(
            UserRepository userRepository,
            RoleRepository roleRepository,
            IdempotencyGuard idempotencyGuard) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public void execute(AssignUserRolesCommand command, String idempotencyKey) {
        idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start AssignUserRoles | actor={} | userId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.userId()));
        if (userRepository.findById(command.userId()).isEmpty()) {
            throw new BusinessException(ErrorCode.IAM_030);
        }
        Set<String> roleCodes = CodeSetUtil.normalize(command.roles());
        if (!roleRepository.findExistingCodes(roleCodes).containsAll(roleCodes)) {
            throw new BusinessException(ErrorCode.IAM_031);
        }
        userRepository.replaceRoles(command.userId(), roleCodes, command.actorId());
        log.info("[ACTION] Complete AssignUserRoles | actor={} | userId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.userId()));
    }
}
