package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.service.RoleDetailView;
import com.eprocure.iam.domain.model.Role;
import com.eprocure.iam.domain.repository.RoleRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListRolesUseCase {
    private static final Logger log = LoggerFactory.getLogger(ListRolesUseCase.class);
    private final RoleRepository roleRepository;

    public ListRolesUseCase(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleDetailView> execute() {
        log.info("[ACTION] Start ListRoles");
        List<RoleDetailView> roles = roleRepository.findAll().stream()
                .map(this::toView)
                .toList();
        log.info("[ACTION] Complete ListRoles | count={}", roles.size());
        return roles;
    }

    private RoleDetailView toView(Role role) {
        return new RoleDetailView(
                role.getCode(),
                role.getName(),
                role.getDescription().orElse(null),
                roleRepository.findPermissionCodesByRoleCode(role.getCode()),
                role.isSystemRole());
    }
}
