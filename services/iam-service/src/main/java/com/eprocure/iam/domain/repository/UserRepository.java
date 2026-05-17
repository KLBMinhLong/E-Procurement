package com.eprocure.iam.domain.repository;

import com.eprocure.iam.domain.model.User;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);

    Optional<User> findByUsernameOrEmail(String usernameOrEmail);

    Set<String> findRoleCodesByUserId(UUID userId);

    Set<String> findPermissionCodesByUserId(UUID userId);

    void updateLastLoginAt(UUID userId, Instant lastLoginAt);
}
