package com.eprocure.pr.infrastructure.persistence.mapper;

import com.eprocure.pr.infrastructure.persistence.entity.CatalogItemDbEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CatalogItemMapper {

    List<CatalogItemDbEntity> search(
            @Param("query") String query,
            @Param("categoryCode") String categoryCode,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    long count(
            @Param("query") String query,
            @Param("categoryCode") String categoryCode
    );
}
