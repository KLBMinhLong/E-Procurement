## SK-03 · Repository & MyBatis Mapper

### Trigger
Agent cần implement data access layer cho một domain entity.

### Inputs Required
- Entity và filter definition
- Schema + table name
- Relations (line items) mapping
- Pagination strategy (RowBounds hoặc LIMIT/OFFSET)

### Rules
```
[R1] KHÔNG dùng JPA/Hibernate — chỉ MyBatis
[R2] Repository Interface nằm trong domain layer (không import Spring/MyBatis)
[R3] Repository Implementation nằm trong infrastructure.persistence
[R4] MyBatis Mapper nằm trong infrastructure.persistence.mapper
[R5] Simple CRUD: annotation trong @Mapper interface
[R6] Complex query (JOIN nhiều bảng, dynamic WHERE): XML mapper file
[R7] Mọi query có WHERE is_deleted = false (soft delete)
[R8] Không viết SQL trong Use Case hay Repository Impl — chỉ trong Mapper
[R9] Pagination: dùng MyBatis RowBounds hoặc LIMIT/OFFSET trong SQL
[R10] @Results / resultMap bắt buộc — không dựa vào auto-mapping
```

### Template — Domain Repository Interface
```java
package com.eprocure.{service}.domain.repository;

import com.eprocure.{service}.domain.model.{Entity};
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Port: {Entity}Repository
 * Domain interface — no framework dependencies.
 */
public interface {Entity}Repository {

    Optional<{Entity}> findById(UUID id);

    List<{Entity}> findByFilter({Entity}Filter filter, int offset, int limit);

    long countByFilter({Entity}Filter filter);

    void save({Entity} entity);   // INSERT or UPDATE (upsert by id)

    void softDelete(UUID id, UUID deletedBy);
}
```

### Template — MyBatis Mapper
```java
package com.eprocure.{service}.infrastructure.persistence.mapper;

import com.eprocure.{service}.infrastructure.persistence.entity.{Entity}Entity;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.FetchType;
import java.util.*;

/**
 * MyBatis Mapper: {Entity}Mapper
 */
@Mapper
public interface {Entity}Mapper {

    @Insert("""
        INSERT INTO {schema}.{table_name} (
            id, {entity}_number, requester_id, department_id,
            status, priority, estimated_total,
            created_by, created_at, updated_by, updated_at, is_deleted
        ) VALUES (
            #{id}, #{entityNumber}, #{requesterId}, #{departmentId},
            #{status}, #{priority}, #{estimatedTotal},
            #{createdBy}, #{createdAt}, #{updatedBy}, #{updatedAt}, false
        )
        """)
    void insert({Entity}Entity entity);

    @Update("""
        UPDATE {schema}.{table_name}
        SET status = #{status},
            updated_by = #{updatedBy},
            updated_at = #{updatedAt}
        WHERE id = #{id} AND is_deleted = false
        """)
    void update({Entity}Entity entity);

    @Select("""
        SELECT * FROM {schema}.{table_name}
        WHERE id = #{id} AND is_deleted = false
        """)
    @Results(id = "{entity}Result", value = {
        @Result(property = "entityNumber", column = "{entity}_number"),
        @Result(property = "requesterId",  column = "requester_id"),
        @Result(property = "departmentId", column = "department_id"),
        @Result(property = "estimatedTotal", column = "estimated_total"),
        @Result(property = "createdBy",    column = "created_by"),
        @Result(property = "createdAt",    column = "created_at"),
        @Result(property = "updatedBy",    column = "updated_by"),
        @Result(property = "updatedAt",    column = "updated_at"),
        @Result(property = "isDeleted",    column = "is_deleted"),
        @Result(property = "lineItems",    column = "id",
                many = @Many(select = "selectLineItemsBy{Entity}Id",
                             fetchType = FetchType.LAZY))
    })
    Optional<{Entity}Entity> findById(UUID id);

    @Update("""
        UPDATE {schema}.{table_name}
        SET is_deleted = true, deleted_at = NOW(), deleted_by = #{deletedBy}
        WHERE id = #{id} AND is_deleted = false
        """)
    void softDelete(@Param("id") UUID id, @Param("deletedBy") UUID deletedBy);
}
```

### Template — XML Mapper (complex query)
```xml
<!-- resources/mapper/{Entity}Mapper.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.eprocure.{service}.infrastructure.persistence.mapper.{Entity}Mapper">

    <select id="findByFilter" resultMap="{entity}Result">
        SELECT e.*
        FROM {schema}.{table_name} e
        WHERE e.is_deleted = false
        <if test="filter.departmentId != null">
            AND e.department_id = #{filter.departmentId}
        </if>
        <if test="filter.status != null">
            AND e.status = #{filter.status}
        </if>
        <if test="filter.requesterId != null">
            AND e.requester_id = #{filter.requesterId}
        </if>
        <if test="filter.fromDate != null">
            AND e.created_at >= #{filter.fromDate}
        </if>
        <if test="filter.toDate != null">
            AND e.created_at &lt;= #{filter.toDate}
        </if>
        ORDER BY e.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>

    <select id="countByFilter" resultType="long">
        SELECT COUNT(*)
        FROM {schema}.{table_name} e
        WHERE e.is_deleted = false
        <if test="filter.departmentId != null">
            AND e.department_id = #{filter.departmentId}
        </if>
        <if test="filter.status != null">
            AND e.status = #{filter.status}
        </if>
    </select>

</mapper>
```

### Checklist
```
[ ] Không dùng JPA/Hibernate
[ ] Mọi query có is_deleted = false
[ ] @Results / resultMap được định nghĩa rõ ràng
[ ] Complex query nằm ở XML mapper
```
