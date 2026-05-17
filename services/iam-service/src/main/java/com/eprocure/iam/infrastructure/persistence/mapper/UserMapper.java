package com.eprocure.iam.infrastructure.persistence.mapper;

import com.eprocure.iam.domain.model.UserStatus;
import com.eprocure.iam.infrastructure.persistence.entity.UserDbEntity;
import com.eprocure.iam.infrastructure.persistence.entity.UserPageDbEntity;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {
    @Select("""
            SELECT id, employee_code, username, email, full_name, phone, avatar_url,
                   department_id, org_node_id, keycloak_username, google_oauth_id, status, two_factor_enabled,
                   two_factor_secret_encrypted, two_factor_pending_secret_encrypted, two_factor_confirmed_at,
                   last_login_at, created_at
            FROM iam.users
            WHERE is_deleted = FALSE
              AND id = #{id}
            """)
    UserDbEntity findById(@Param("id") UUID id);

    @Select("""
            SELECT id, employee_code, username, email, full_name, phone, avatar_url,
                   department_id, org_node_id, keycloak_username, google_oauth_id, status, two_factor_enabled,
                   two_factor_secret_encrypted, two_factor_pending_secret_encrypted, two_factor_confirmed_at,
                   last_login_at, created_at
            FROM iam.users
            WHERE is_deleted = FALSE
              AND (LOWER(username) = LOWER(#{login}) OR LOWER(email) = LOWER(#{login}))
            LIMIT 1
            """)
    UserDbEntity findByUsernameOrEmail(@Param("login") String login);

    @Select("""
            SELECT id, employee_code, username, email, full_name, phone, avatar_url,
                   department_id, org_node_id, keycloak_username, google_oauth_id, status, two_factor_enabled,
                   two_factor_secret_encrypted, two_factor_pending_secret_encrypted, two_factor_confirmed_at,
                   last_login_at, created_at
            FROM iam.users
            WHERE is_deleted = FALSE
              AND (
                    LOWER(employee_code) = LOWER(#{employeeCode})
                 OR LOWER(username) = LOWER(#{username})
                 OR LOWER(email) = LOWER(#{email})
              )
            LIMIT 1
            """)
    UserDbEntity findByEmployeeCodeOrUsernameOrEmail(
            @Param("employeeCode") String employeeCode,
            @Param("username") String username,
            @Param("email") String email);

    @Select("""
            SELECT id, employee_code, username, email, full_name, phone, avatar_url,
                   department_id, org_node_id, keycloak_username, google_oauth_id, status, two_factor_enabled,
                   two_factor_secret_encrypted, two_factor_pending_secret_encrypted, two_factor_confirmed_at,
                   last_login_at, created_at
            FROM iam.users
            WHERE is_deleted = FALSE
              AND google_oauth_id = #{googleOauthId}
            LIMIT 1
            """)
    UserDbEntity findByGoogleOauthId(@Param("googleOauthId") String googleOauthId);

    List<UserPageDbEntity> findPage(
            @Param("status") UserStatus status,
            @Param("departmentId") UUID departmentId,
            @Param("roleCode") String roleCode,
            @Param("query") String query,
            @Param("sortColumn") String sortColumn,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("limit") int limit);

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

    @Insert("""
            INSERT INTO iam.users (
                id, employee_code, username, email, full_name, phone, avatar_url,
                department_id, org_node_id, keycloak_username, status, two_factor_enabled, created_by
            ) VALUES (
                #{entity.id}, #{entity.employeeCode}, #{entity.username}, #{entity.email}, #{entity.fullName}, #{entity.phone}, #{entity.avatarUrl},
                #{entity.departmentId}, #{entity.orgNodeId}, #{entity.keycloakUsername}, #{entity.status}, #{entity.twoFactorEnabled}, #{actorId}
            )
            """)
    void insert(@Param("entity") UserDbEntity entity, @Param("actorId") UUID actorId);

    @Update("""
            UPDATE iam.users
            SET full_name = #{entity.fullName},
                phone = #{entity.phone},
                department_id = #{entity.departmentId},
                org_node_id = #{entity.orgNodeId},
                updated_by = #{actorId}
            WHERE id = #{entity.id}
              AND is_deleted = FALSE
            """)
    void update(@Param("entity") UserDbEntity entity, @Param("actorId") UUID actorId);

    @Update("""
            UPDATE iam.users
            SET status = #{status},
                updated_by = #{actorId}
            WHERE id = #{userId}
              AND is_deleted = FALSE
            """)
    void updateStatus(
            @Param("userId") UUID userId,
            @Param("status") UserStatus status,
            @Param("actorId") UUID actorId);

    void softDeleteUserRolesNotIn(
            @Param("userId") UUID userId,
            @Param("roleCodes") Set<String> roleCodes,
            @Param("actorId") UUID actorId);

    @Insert("""
            INSERT INTO iam.user_roles (id, user_id, role_id, created_by, updated_by, is_deleted, deleted_at, deleted_by)
            SELECT gen_random_uuid(), #{userId}, r.id, #{actorId}, #{actorId}, FALSE, NULL, NULL
            FROM iam.roles r
            WHERE r.is_deleted = FALSE
              AND UPPER(r.code) = UPPER(#{roleCode})
            ON CONFLICT (user_id, role_id) DO UPDATE
            SET is_deleted = FALSE,
                deleted_at = NULL,
                deleted_by = NULL,
                updated_by = #{actorId},
                updated_at = NOW()
            """)
    void upsertUserRole(
            @Param("userId") UUID userId,
            @Param("roleCode") String roleCode,
            @Param("actorId") UUID actorId);

    @Update("""
            UPDATE iam.users
            SET last_login_at = #{lastLoginAt},
                updated_at = NOW()
            WHERE id = #{userId}
              AND is_deleted = FALSE
            """)
    void updateLastLoginAt(@Param("userId") UUID userId, @Param("lastLoginAt") Instant lastLoginAt);

    @Update("""
            UPDATE iam.users
            SET google_oauth_id = #{googleOauthId},
                updated_by = #{actorId}
            WHERE id = #{userId}
              AND is_deleted = FALSE
            """)
    void linkGoogleOauthId(
            @Param("userId") UUID userId,
            @Param("googleOauthId") String googleOauthId,
            @Param("actorId") UUID actorId);

    @Update("""
            UPDATE iam.users
            SET two_factor_pending_secret_encrypted = #{encryptedSecret},
                updated_by = #{actorId}
            WHERE id = #{userId}
              AND is_deleted = FALSE
            """)
    void stageTwoFactorSecret(
            @Param("userId") UUID userId,
            @Param("encryptedSecret") String encryptedSecret,
            @Param("actorId") UUID actorId);

    @Update("""
            UPDATE iam.users
            SET two_factor_secret_encrypted = #{encryptedSecret},
                two_factor_pending_secret_encrypted = NULL,
                two_factor_enabled = TRUE,
                two_factor_confirmed_at = #{confirmedAt},
                two_factor_backup_codes_hash = CAST(#{backupCodesHashJson} AS JSONB),
                updated_by = #{actorId}
            WHERE id = #{userId}
              AND is_deleted = FALSE
            """)
    void confirmTwoFactor(
            @Param("userId") UUID userId,
            @Param("encryptedSecret") String encryptedSecret,
            @Param("backupCodesHashJson") String backupCodesHashJson,
            @Param("confirmedAt") Instant confirmedAt,
            @Param("actorId") UUID actorId);
}
