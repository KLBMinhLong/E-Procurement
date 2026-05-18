package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.UpdateRolePermissionsCommand;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.application.service.PermissionResolutionService;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.repository.PermissionRepository;
import com.eprocure.iam.domain.repository.RoleRepository;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateRolePermissionsUseCase {
    private static final Logger log = LogManager.getLogger(UpdateRolePermissionsUseCase.class);
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final IdempotencyGuard idempotencyGuard;
    private final PermissionResolutionService permissionResolutionService;

    public UpdateRolePermissionsUseCase(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            IdempotencyGuard idempotencyGuard,
            PermissionResolutionService permissionResolutionService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.idempotencyGuard = idempotencyGuard;
        this.permissionResolutionService = permissionResolutionService;
    }

    @Transactional
    public void execute(UpdateRolePermissionsCommand command, String idempotencyKey) {
        idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start UpdateRolePermissions | actor={} | roleCode={}",
                LogMaskingUtil.maskId(command.actorId()),
                command.roleCode());
        if (roleRepository.findByCode(command.roleCode()).isEmpty()) {
            throw new BusinessException(ErrorCode.IAM_031);
        }
        Set<String> permissionCodes = CodeSetUtil.normalize(command.permissions());
        if (!permissionRepository.findExistingCodes(permissionCodes).containsAll(permissionCodes)) {
            throw new BusinessException(ErrorCode.IAM_032);
        }
        roleRepository.replacePermissions(command.roleCode(), permissionCodes, command.actorId());
        permissionResolutionService.refreshRolePermissions(command.roleCode(), permissionCodes);
        log.info("[ACTION] Complete UpdateRolePermissions | actor={} | roleCode={}",
                LogMaskingUtil.maskId(command.actorId()),
                command.roleCode());
    }
}
