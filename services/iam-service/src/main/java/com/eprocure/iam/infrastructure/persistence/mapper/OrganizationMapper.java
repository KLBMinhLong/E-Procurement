package com.eprocure.iam.infrastructure.persistence.mapper;

import com.eprocure.iam.infrastructure.persistence.entity.DepartmentDbEntity;
import com.eprocure.iam.infrastructure.persistence.entity.UserDbEntity;
import com.eprocure.iam.infrastructure.persistence.entity.UserPageDbEntity;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrganizationMapper {
    @Select("""
            SELECT id, code, name, parent_id, head_user_id, created_at
            FROM iam.departments
            WHERE is_deleted = FALSE
            ORDER BY name
            """)
    List<DepartmentDbEntity> findAllDepartments();

    @Select("""
            SELECT id, code, name, parent_id, head_user_id, created_at
            FROM iam.departments
            WHERE is_deleted = FALSE
              AND id = #{departmentId}
            """)
    DepartmentDbEntity findDepartmentById(@Param("departmentId") UUID departmentId);

    @Select("""
            SELECT COUNT(*)
            FROM iam.users u
            WHERE u.is_deleted = FALSE
              AND u.status = 'ACTIVE'
              AND u.department_id = #{departmentId}
            """)
    long countActiveMembersByDepartmentId(@Param("departmentId") UUID departmentId);

    List<UserPageDbEntity> findDepartmentMembers(
            @Param("departmentId") UUID departmentId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    List<UserDbEntity> findApprovers(
            @Param("roleCode") String roleCode,
            @Param("departmentId") UUID departmentId,
            @Param("excludedUserId") UUID excludedUserId,
            @Param("limit") int limit);

    List<UserDbEntity> findApproversByPermission(
            @Param("permissionCode") String permissionCode,
            @Param("departmentId") UUID departmentId,
            @Param("excludedUserId") UUID excludedUserId,
            @Param("limit") int limit);
}
