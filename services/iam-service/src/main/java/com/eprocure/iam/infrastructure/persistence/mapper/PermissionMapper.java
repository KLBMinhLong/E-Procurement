package com.eprocure.iam.infrastructure.persistence.mapper;

import com.eprocure.iam.infrastructure.persistence.entity.PermissionDbEntity;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PermissionMapper {
    @Select("""
            SELECT id, code, name, description, service, created_at
            FROM iam.permissions
            WHERE is_deleted = FALSE
            ORDER BY service, code
            """)
    List<PermissionDbEntity> findAll();

    List<String> findExistingCodes(@Param("codes") Set<String> codes);
}
