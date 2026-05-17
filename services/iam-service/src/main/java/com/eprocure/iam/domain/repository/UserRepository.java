package com.eprocure.iam.domain.repository;

import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.model.SortDirection;
import com.eprocure.iam.domain.model.UserSearchCriteria;
import com.eprocure.iam.domain.model.UserSort;
import com.eprocure.iam.domain.model.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);

    Optional<User> findByUsernameOrEmail(String usernameOrEmail);

    Optional<User> findByGoogleOauthId(String googleOauthId);

    Optional<User> findByEmployeeCodeOrUsernameOrEmail(String employeeCode, String username, String email);

    Page<User> findPage(UserSearchCriteria criteria, UserSort sort, SortDirection direction, int offset, int limit);

    Set<String> findRoleCodesByUserId(UUID userId);

    Set<String> findPermissionCodesByUserId(UUID userId);

    void save(User user, UUID actorId);

    void update(User user, UUID actorId);

    void updateStatus(UUID userId, UserStatus status, UUID actorId);

    void replaceRoles(UUID userId, Set<String> roleCodes, UUID actorId);

    void updateLastLoginAt(UUID userId, Instant lastLoginAt);

    void linkGoogleOauthId(UUID userId, String googleOauthId, UUID actorId);

    void stageTwoFactorSecret(UUID userId, String encryptedSecret, UUID actorId);

    void confirmTwoFactor(
            UUID userId,
            String encryptedSecret,
            List<String> backupCodeHashes,
            Instant confirmedAt,
            UUID actorId);
}
