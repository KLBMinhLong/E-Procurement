package com.eprocure.iam.infrastructure.persistence.repository;

import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.model.SortDirection;
import com.eprocure.iam.domain.model.UserSearchCriteria;
import com.eprocure.iam.domain.model.UserSort;
import com.eprocure.iam.domain.model.UserStatus;
import com.eprocure.iam.domain.repository.Page;
import com.eprocure.iam.domain.repository.UserRepository;
import com.eprocure.iam.infrastructure.persistence.entity.UserDbEntity;
import com.eprocure.iam.infrastructure.persistence.entity.UserPageDbEntity;
import com.eprocure.iam.infrastructure.persistence.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl implements UserRepository {
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public UserRepositoryImpl(UserMapper userMapper, @Qualifier("domainObjectMapper") ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(userMapper.findById(id)).map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return Optional.ofNullable(userMapper.findByUsernameOrEmail(usernameOrEmail)).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmployeeCodeOrUsernameOrEmail(String employeeCode, String username, String email) {
        return Optional.ofNullable(userMapper.findByEmployeeCodeOrUsernameOrEmail(employeeCode, username, email))
                .map(this::toDomain);
    }

    @Override
    public Page<User> findPage(UserSearchCriteria criteria, UserSort sort, SortDirection direction, int offset, int limit) {
        List<UserPageDbEntity> rows = userMapper.findPage(
                criteria.status(),
                criteria.departmentId(),
                criteria.roleCode(),
                criteria.query(),
                sort.name(),
                direction.name(),
                offset,
                limit);
        long totalElements = rows.isEmpty() ? 0 : rows.get(0).totalElements;
        return new Page<>(rows.stream().map(this::toDomain).toList(), totalElements);
    }

    @Override
    public Set<String> findRoleCodesByUserId(UUID userId) {
        return Set.copyOf(Optional.ofNullable(userMapper.findRoleCodesByUserId(userId)).orElseGet(List::of));
    }

    @Override
    public Set<String> findPermissionCodesByUserId(UUID userId) {
        return Set.copyOf(Optional.ofNullable(userMapper.findPermissionCodesByUserId(userId)).orElseGet(List::of));
    }

    @Override
    public void save(User user, UUID actorId) {
        userMapper.insert(toEntity(user), actorId);
    }

    @Override
    public void update(User user, UUID actorId) {
        userMapper.update(toEntity(user), actorId);
    }

    @Override
    public void updateStatus(UUID userId, UserStatus status, UUID actorId) {
        userMapper.updateStatus(userId, status, actorId);
    }

    @Override
    public void replaceRoles(UUID userId, Set<String> roleCodes, UUID actorId) {
        userMapper.softDeleteUserRolesNotIn(userId, roleCodes, actorId);
        roleCodes.forEach(roleCode -> userMapper.upsertUserRole(userId, roleCode, actorId));
    }

    @Override
    public void updateLastLoginAt(UUID userId, Instant lastLoginAt) {
        userMapper.updateLastLoginAt(userId, lastLoginAt);
    }

    private User toDomain(UserDbEntity entity) {
        return objectMapper.convertValue(entity, User.class);
    }

    private UserDbEntity toEntity(User user) {
        return objectMapper.convertValue(user, UserDbEntity.class);
    }
}
