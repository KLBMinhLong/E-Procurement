package com.eprocure.iam.infrastructure.persistence.mapper;

import com.eprocure.iam.infrastructure.persistence.entity.DepartmentDbEntity;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DepartmentMapper {
    @Select("""
            SELECT id, code, name, parent_id, head_user_id, created_at
            FROM iam.departments
            WHERE is_deleted = FALSE
              AND id = #{id}
            """)
    DepartmentDbEntity findById(@Param("id") UUID id);
}
