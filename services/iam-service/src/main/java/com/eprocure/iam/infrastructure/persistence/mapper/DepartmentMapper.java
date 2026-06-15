package com.eprocure.iam.infrastructure.persistence.mapper;

import com.eprocure.iam.infrastructure.persistence.entity.DepartmentDbEntity;
import java.time.Instant;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DepartmentMapper {
    @Select("""
            SELECT id, code, name, parent_id, head_user_id, created_at, updated_at, is_deleted AS deleted
            FROM iam.departments
            WHERE is_deleted = FALSE
              AND id = #{id}
            """)
    DepartmentDbEntity findById(@Param("id") UUID id);

    @Select("""
            SELECT id, code, name, parent_id, head_user_id, created_at, updated_at, is_deleted AS deleted
            FROM iam.departments
            WHERE id = #{id}
            """)
    DepartmentDbEntity findByIdIncludingInactive(@Param("id") UUID id);

    @Select("""
            SELECT id, code, name, parent_id, head_user_id, created_at, updated_at, is_deleted AS deleted
            FROM iam.departments
            WHERE is_deleted = FALSE
              AND LOWER(code) = LOWER(#{code})
            LIMIT 1
            """)
    DepartmentDbEntity findByCode(@Param("code") String code);

    @Select("""
            SELECT COUNT(1) > 0
            FROM iam.departments
            WHERE is_deleted = FALSE
              AND LOWER(code) = LOWER(#{code})
            """)
    boolean existsActiveByCode(@Param("code") String code);

    @Select("""
            SELECT COUNT(1) > 0
            FROM iam.departments
            WHERE is_deleted = FALSE
              AND LOWER(code) = LOWER(#{code})
              AND id <> #{excludedId}
            """)
    boolean existsActiveByCodeExceptId(@Param("code") String code, @Param("excludedId") UUID excludedId);

    @Select("""
            WITH RECURSIVE descendants AS (
                SELECT id, parent_id
                FROM iam.departments
                WHERE is_deleted = FALSE
                  AND parent_id = #{departmentId}
                UNION ALL
                SELECT d.id, d.parent_id
                FROM iam.departments d
                JOIN descendants child ON d.parent_id = child.id
                WHERE d.is_deleted = FALSE
            )
            SELECT COUNT(1) > 0
            FROM descendants
            WHERE id = #{candidateParentId}
            """)
    boolean isDescendant(
            @Param("candidateParentId") UUID candidateParentId,
            @Param("departmentId") UUID departmentId);

    @Select("""
            SELECT COUNT(*)
            FROM iam.users
            WHERE is_deleted = FALSE
              AND status = 'ACTIVE'
              AND department_id = #{departmentId}
            """)
    long countActiveMembersByDepartmentId(@Param("departmentId") UUID departmentId);

    @Select("""
            SELECT COUNT(*)
            FROM iam.departments
            WHERE is_deleted = FALSE
              AND parent_id = #{departmentId}
            """)
    long countActiveChildrenByDepartmentId(@Param("departmentId") UUID departmentId);

    @Insert("""
            INSERT INTO iam.departments (
                id,
                code,
                name,
                parent_id,
                head_user_id,
                created_at,
                updated_at,
                created_by
            )
            VALUES (
                #{entity.id},
                #{entity.code},
                #{entity.name},
                #{entity.parentId,jdbcType=OTHER},
                #{entity.headUserId,jdbcType=OTHER},
                #{entity.createdAt},
                #{entity.updatedAt},
                #{actorId}
            )
            """)
    void insert(@Param("entity") DepartmentDbEntity entity, @Param("actorId") UUID actorId);

    @Update("""
            UPDATE iam.departments
            SET code = #{entity.code},
                name = #{entity.name},
                parent_id = #{entity.parentId,jdbcType=OTHER},
                head_user_id = #{entity.headUserId,jdbcType=OTHER},
                updated_by = #{actorId},
                updated_at = #{entity.updatedAt}
            WHERE id = #{entity.id}
              AND is_deleted = FALSE
            """)
    void update(@Param("entity") DepartmentDbEntity entity, @Param("actorId") UUID actorId);

    @Update("""
            UPDATE iam.departments
            SET is_deleted = TRUE,
                deleted_at = #{deletedAt},
                deleted_by = #{actorId},
                updated_by = #{actorId},
                updated_at = #{deletedAt}
            WHERE id = #{departmentId}
              AND is_deleted = FALSE
            """)
    void deactivate(
            @Param("departmentId") UUID departmentId,
            @Param("actorId") UUID actorId,
            @Param("deletedAt") Instant deletedAt);
}
