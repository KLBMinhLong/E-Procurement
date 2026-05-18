package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.AssignUserRolesCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.SessionService;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.repository.RoleRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignUserRolesUseCase {
    private static final Logger log = LogManager.getLogger(AssignUserRolesUseCase.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final IdempotencyGuard idempotencyGuard;
    private final SessionService sessionService;

    public AssignUserRolesUseCase(
            UserRepository userRepository,
            RoleRepository roleRepository,
            IdempotencyGuard idempotencyGuard,
            SessionService sessionService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.idempotencyGuard = idempotencyGuard;
        this.sessionService = sessionService;
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
        sessionService.evictActiveCacheForUser(command.userId());
        log.info("[ACTION] Complete AssignUserRoles | actor={} | userId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.userId()));
    }
}
