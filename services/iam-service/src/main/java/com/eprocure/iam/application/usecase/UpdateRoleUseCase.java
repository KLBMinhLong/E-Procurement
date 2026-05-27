package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.UpdateRoleCommand;
import com.eprocure.iam.application.service.PermissionResolutionService;
import com.eprocure.iam.application.service.RoleDetailView;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.Role;
import com.eprocure.iam.domain.repository.RoleRepository;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateRoleUseCase {
    private static final Logger log = LogManager.getLogger(UpdateRoleUseCase.class);
    private final RoleRepository roleRepository;
    private final PermissionResolutionService permissionResolutionService;

    public UpdateRoleUseCase(RoleRepository roleRepository, PermissionResolutionService permissionResolutionService) {
        this.roleRepository = roleRepository;
        this.permissionResolutionService = permissionResolutionService;
    }

    @Transactional
    public RoleDetailView execute(UpdateRoleCommand command) {
        log.info("[ACTION] Start UpdateRole | actor={} | currentCode={} | newCode={}",
                LogMaskingUtil.maskId(command.actorId()),
                command.currentCode(),
                command.newCode());

        Role role = roleRepository.findByCode(command.currentCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_031));

        if (role.isSystemRole()) {
            throw new BusinessException(ErrorCode.IAM_005);
        }

        String newCodeUpper = command.newCode().trim().toUpperCase();
        if (!newCodeUpper.equalsIgnoreCase(command.currentCode())) {
            if (roleRepository.findByCode(newCodeUpper).isPresent()) {
                throw new BusinessException(ErrorCode.IAM_009);
            }
        }

        role.updateDetails(command.newCode(), command.name(), command.description());
        roleRepository.update(role, command.actorId());

        // Cache eviction & refresh
        permissionResolutionService.evictRolePermissions(command.currentCode());
        Set<String> permissionCodes = roleRepository.findPermissionCodesByRoleCode(role.getCode());
        permissionResolutionService.refreshRolePermissions(role.getCode(), permissionCodes);

        RoleDetailView view = new RoleDetailView(
                role.getCode(),
                role.getName(),
                role.getDescription().orElse(null),
                permissionCodes,
                role.isSystemRole());

        log.info("[ACTION] Complete UpdateRole | actor={} | roleCode={}",
                LogMaskingUtil.maskId(command.actorId()),
                role.getCode());
        return view;
    }
}
