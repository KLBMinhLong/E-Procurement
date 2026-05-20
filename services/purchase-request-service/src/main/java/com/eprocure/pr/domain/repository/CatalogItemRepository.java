package com.eprocure.pr.domain.repository;

import com.eprocure.pr.domain.model.CatalogItem;
import java.util.List;

public interface CatalogItemRepository {
    List<CatalogItem> search(String query, String categoryCode, int offset, int limit);
    long count(String query, String categoryCode);
}
