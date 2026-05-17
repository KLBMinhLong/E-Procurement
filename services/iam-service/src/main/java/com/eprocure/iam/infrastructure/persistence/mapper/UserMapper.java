package com.eprocure.iam.infrastructure.persistence.mapper;

import com.eprocure.iam.infrastructure.persistence.entity.UserDbEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {
    @Select("""
            SELECT id, employee_code, username, email, full_name, phone, avatar_url,
                   department_id, org_node_id, status, two_factor_enabled, last_login_at, created_at
            FROM iam.users
            WHERE is_deleted = FALSE
              AND id = #{id}
            """)
    UserDbEntity findById(@Param("id") UUID id);

    @Select("""
            SELECT id, employee_code, username, email, full_name, phone, avatar_url,
                   department_id, org_node_id, status, two_factor_enabled, last_login_at, created_at
            FROM iam.users
            WHERE is_deleted = FALSE
              AND (LOWER(username) = LOWER(#{login}) OR LOWER(email) = LOWER(#{login}))
            LIMIT 1
            """)
    UserDbEntity findByUsernameOrEmail(@Param("login") String login);

    @Select("""
            SELECT r.code
            FROM iam.user_roles ur
            JOIN iam.roles r ON r.id = ur.role_id AND r.is_deleted = FALSE
            WHERE ur.user_id = #{userId}
              AND ur.is_deleted = FALSE
            ORDER BY r.code
            """)
    List<String> findRoleCodesByUserId(@Param("userId") UUID userId);

    @Select("""
            SELECT DISTINCT p.code
            FROM iam.user_roles ur
            JOIN iam.roles r ON r.id = ur.role_id AND r.is_deleted = FALSE
            JOIN iam.role_permissions rp ON rp.role_id = r.id AND rp.is_deleted = FALSE
            JOIN iam.permissions p ON p.id = rp.permission_id AND p.is_deleted = FALSE
            WHERE ur.user_id = #{userId}
              AND ur.is_deleted = FALSE
            ORDER BY p.code
            """)
    List<String> findPermissionCodesByUserId(@Param("userId") UUID userId);

    @Update("""
            UPDATE iam.users
            SET last_login_at = #{lastLoginAt},
                updated_at = NOW()
            WHERE id = #{userId}
              AND is_deleted = FALSE
            """)
    void updateLastLoginAt(@Param("userId") UUID userId, @Param("lastLoginAt") Instant lastLoginAt);
}
