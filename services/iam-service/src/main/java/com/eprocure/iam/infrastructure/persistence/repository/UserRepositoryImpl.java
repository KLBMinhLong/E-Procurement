package com.eprocure.iam.infrastructure.persistence.repository;

import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.UserRepository;
import com.eprocure.iam.infrastructure.persistence.entity.UserDbEntity;
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
    public Set<String> findRoleCodesByUserId(UUID userId) {
        return Set.copyOf(Optional.ofNullable(userMapper.findRoleCodesByUserId(userId)).orElseGet(List::of));
    }

    @Override
    public Set<String> findPermissionCodesByUserId(UUID userId) {
        return Set.copyOf(Optional.ofNullable(userMapper.findPermissionCodesByUserId(userId)).orElseGet(List::of));
    }

    @Override
    public void updateLastLoginAt(UUID userId, Instant lastLoginAt) {
        userMapper.updateLastLoginAt(userId, lastLoginAt);
    }

    private User toDomain(UserDbEntity entity) {
        return objectMapper.convertValue(entity, User.class);
    }
}
