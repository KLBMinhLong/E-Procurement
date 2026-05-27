package com.eprocure.iam.application.usecase;

import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.domain.repository.RoleRepository;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetRolePermissionsUseCase {
    private static final Logger log = LogManager.getLogger(GetRolePermissionsUseCase.class);
    private final RoleRepository roleRepository;

    public GetRolePermissionsUseCase(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public Set<String> execute(String roleCode) {
        log.info("[ACTION] Start GetRolePermissions | roleCode={}", roleCode);
        if (roleRepository.findByCode(roleCode).isEmpty()) {
            throw new BusinessException(ErrorCode.IAM_031);
        }
        Set<String> permissions = roleRepository.findPermissionCodesByRoleCode(roleCode);
        log.info("[ACTION] Complete GetRolePermissions | roleCode={} | count={}", roleCode, permissions.size());
        return permissions;
    }
}
