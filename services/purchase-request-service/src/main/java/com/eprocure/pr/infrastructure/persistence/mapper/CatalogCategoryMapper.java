package com.eprocure.pr.infrastructure.persistence.mapper;

import com.eprocure.pr.infrastructure.persistence.entity.CatalogCategoryDbEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CatalogCategoryMapper {

    @Select("SELECT * FROM pr.catalog_categories WHERE is_deleted = false ORDER BY code ASC")
    List<CatalogCategoryDbEntity> findAll();

}
