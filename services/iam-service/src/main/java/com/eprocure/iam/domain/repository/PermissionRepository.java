package com.eprocure.iam.domain.repository;

import com.eprocure.iam.domain.model.Permission;
import java.util.List;
import java.util.Set;

public interface PermissionRepository {
    List<Permission> findAll();

    Set<String> findExistingCodes(Set<String> codes);
}
