package com.eprocure.admin.infrastructure.persistence.mapper;

import com.eprocure.admin.domain.model.AuditLogFilter;
import com.eprocure.admin.infrastructure.persistence.entity.AuditLogDbEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuditLogMapper {
    List<AuditLogDbEntity> findByFilter(@Param("filter") AuditLogFilter filter);

    List<AuditLogDbEntity> findForExport(@Param("filter") AuditLogFilter filter, @Param("limit") int limit);
}
