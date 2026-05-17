package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.CreateRoleCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.RoleDetailView;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.Role;
import com.eprocure.iam.domain.repository.PermissionRepository;
import com.eprocure.iam.domain.repository.RoleRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateRoleUseCase {
    private static final Logger log = LogManager.getLogger(CreateRoleUseCase.class);
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final IdempotencyGuard idempotencyGuard;

    public CreateRoleUseCase(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            IdempotencyGuard idempotencyGuard) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public RoleDetailView execute(CreateRoleCommand command, String idempotencyKey) {
        idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start CreateRole | actor={} | roleCode={}",
                LogMaskingUtil.maskId(command.actorId()),
                command.code());
        if (roleRepository.findByCode(command.code()).isPresent()) {
            throw new BusinessException(ErrorCode.IAM_009);
        }
        Set<String> permissionCodes = CodeSetUtil.normalize(command.permissions());
        validatePermissions(permissionCodes);
        Role role = Role.create(UUID.randomUUID(), command.code(), command.name(), command.description(), false, Instant.now());
        roleRepository.save(role, permissionCodes, command.actorId());
        RoleDetailView view = new RoleDetailView(
                role.getCode(),
                role.getName(),
                role.getDescription().orElse(null),
                roleRepository.findPermissionCodesByRoleCode(role.getCode()),
                role.isSystemRole());
        log.info("[ACTION] Complete CreateRole | actor={} | roleCode={}",
                LogMaskingUtil.maskId(command.actorId()),
                role.getCode());
        return view;
    }

    private void validatePermissions(Set<String> permissionCodes) {
        if (!permissionRepository.findExistingCodes(permissionCodes).containsAll(permissionCodes)) {
            throw new BusinessException(ErrorCode.IAM_032);
        }
    }
}
