package com.eprocure.iam.infrastructure.persistence.mapper;

import com.eprocure.iam.infrastructure.persistence.entity.RoleDbEntity;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;


@Mapper
public interface RoleMapper {
    @Select("""
            SELECT id, code, name, description, is_system_role AS system_role, created_at
            FROM iam.roles
            WHERE is_deleted = FALSE
            ORDER BY code
            """)
    List<RoleDbEntity> findAll();

    @Select("""
            SELECT id, code, name, description, is_system_role AS system_role, created_at
            FROM iam.roles
            WHERE is_deleted = FALSE
              AND UPPER(code) = UPPER(#{code})
            LIMIT 1
            """)
    RoleDbEntity findByCode(@Param("code") String code);

    List<String> findExistingCodes(@Param("codes") Set<String> codes);

    @Select("""
            SELECT p.code
            FROM iam.roles r
            JOIN iam.role_permissions rp ON rp.role_id = r.id AND rp.is_deleted = FALSE
            JOIN iam.permissions p ON p.id = rp.permission_id AND p.is_deleted = FALSE
            WHERE r.is_deleted = FALSE
              AND UPPER(r.code) = UPPER(#{code})
            ORDER BY p.code
            """)
    List<String> findPermissionCodesByRoleCode(@Param("code") String code);

    @Insert("""
            INSERT INTO iam.roles (id, code, name, description, is_system_role, created_by)
            VALUES (#{entity.id}, #{entity.code}, #{entity.name}, #{entity.description}, #{entity.systemRole}, #{actorId})
            """)
    void insert(@Param("entity") RoleDbEntity entity, @Param("actorId") UUID actorId);

    void softDeleteRolePermissionsNotIn(
            @Param("roleCode") String roleCode,
            @Param("permissionCodes") Set<String> permissionCodes,
            @Param("actorId") UUID actorId);

    @Insert("""
            INSERT INTO iam.role_permissions (id, role_id, permission_id, created_by, updated_by, is_deleted, deleted_at, deleted_by)
            SELECT gen_random_uuid(), r.id, p.id, #{actorId}, #{actorId}, FALSE, NULL, NULL
            FROM iam.roles r
            JOIN iam.permissions p ON p.is_deleted = FALSE AND UPPER(p.code) = UPPER(#{permissionCode})
            WHERE r.is_deleted = FALSE
              AND UPPER(r.code) = UPPER(#{roleCode})
            ON CONFLICT (role_id, permission_id) DO UPDATE
            SET is_deleted = FALSE,
                deleted_at = NULL,
                deleted_by = NULL,
                updated_by = #{actorId},
                updated_at = NOW()
            """)
    void upsertRolePermission(
            @Param("roleCode") String roleCode,
            @Param("permissionCode") String permissionCode,
            @Param("actorId") UUID actorId);

    @Update("""
            UPDATE iam.roles
            SET code = #{entity.code},
                name = #{entity.name},
                description = #{entity.description},
                updated_by = #{actorId},
                updated_at = NOW()
            WHERE id = #{entity.id}
              AND is_deleted = FALSE
            """)
    void update(@Param("entity") RoleDbEntity entity, @Param("actorId") UUID actorId);
}
