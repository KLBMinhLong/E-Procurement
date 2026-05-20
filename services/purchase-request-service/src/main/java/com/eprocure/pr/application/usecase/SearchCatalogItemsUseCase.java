package com.eprocure.pr.application.usecase;

import com.eprocure.pr.application.port.in.SearchCatalogItemsQuery;
import com.eprocure.pr.common.util.LogMaskingUtil;
import com.eprocure.pr.domain.model.CatalogItem;
import com.eprocure.pr.domain.repository.CatalogItemRepository;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchCatalogItemsUseCase {
    private static final Logger log = LogManager.getLogger(SearchCatalogItemsUseCase.class);

    private final CatalogItemRepository itemRepository;

    public SearchCatalogItemsUseCase(CatalogItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public PagedResult<CatalogItem> search(SearchCatalogItemsQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        log.info("[ACTION] Start SearchCatalogItems | userId={} | q={} | category={} | page={} | size={}",
                LogMaskingUtil.maskId(query.actorId()), query.q(), query.categoryCode(), query.page(), query.size());

        List<CatalogItem> items = itemRepository.search(query.q(), query.categoryCode(), query.offset(), query.size());
        long totalCount = itemRepository.count(query.q(), query.categoryCode());

        log.info("[ACTION] Complete SearchCatalogItems | userId={} | totalCount={}",
                LogMaskingUtil.maskId(query.actorId()), totalCount);

        return new PagedResult<>(items, totalCount, query.page(), query.size());
    }

    public record PagedResult<T>(
            List<T> content,
            long totalElements,
            int page,
            int size
    ) {
        public int totalPages() {
            return size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        }

        public boolean isFirst() { return page <= 1; }

        public boolean isLast() { return page >= totalPages(); }
    }
}
