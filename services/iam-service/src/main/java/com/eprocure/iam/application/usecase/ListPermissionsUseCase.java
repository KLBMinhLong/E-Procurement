package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.service.PermissionView;
import com.eprocure.iam.domain.model.Permission;
import com.eprocure.iam.domain.repository.PermissionRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListPermissionsUseCase {
    private static final Logger log = LoggerFactory.getLogger(ListPermissionsUseCase.class);
    private final PermissionRepository permissionRepository;

    public ListPermissionsUseCase(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, List<PermissionView>> execute() {
        log.info("[ACTION] Start ListPermissions");
        Map<String, List<PermissionView>> grouped = permissionRepository.findAll().stream()
                .collect(LinkedHashMap::new, this::addPermission, Map::putAll);
        log.info("[ACTION] Complete ListPermissions | serviceCount={}", grouped.size());
        return grouped;
    }

    private void addPermission(Map<String, List<PermissionView>> grouped, Permission permission) {
        grouped.compute(permission.getService(), (service, current) -> {
            PermissionView view = new PermissionView(
                    permission.getCode(),
                    permission.getName(),
                    permission.getDescription().orElse(null),
                    permission.getService());
            if (current == null) {
                return List.of(view);
            }
            return java.util.stream.Stream.concat(current.stream(), java.util.stream.Stream.of(view)).toList();
        });
    }
}
