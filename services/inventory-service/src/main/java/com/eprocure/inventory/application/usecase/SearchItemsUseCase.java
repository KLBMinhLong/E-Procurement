package com.eprocure.inventory.application.usecase;

import com.eprocure.inventory.application.port.in.SearchItemsQuery;
import com.eprocure.inventory.application.service.CatalogItemView;
import com.eprocure.inventory.application.service.PageMeta;
import com.eprocure.inventory.application.service.PageResult;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.eprocure.inventory.domain.repository.ItemFilter;
import com.eprocure.inventory.domain.repository.ItemRepository;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchItemsUseCase {
    private static final Logger log = LogManager.getLogger(SearchItemsUseCase.class);
    private static final String DEFAULT_SORT = "name,asc";

    private final ItemRepository itemRepository;

    public SearchItemsUseCase(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<CatalogItemView> execute(SearchItemsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        ItemFilter filter = new ItemFilter(
                query.query(),
                query.categoryCode(),
                query.active(),
                query.belowReorder(),
                query.page(),
                query.size(),
                (query.page() - 1) * query.size());

        log.info("[ACTION] Start SearchItems | userId={} | page={} | size={}",
                LogMaskingUtil.maskId(query.actorId()),
                query.page(),
                query.size());
        var items = itemRepository.findByFilter(filter).stream()
                .map(item -> CatalogItemView.from(item, itemRepository.findStockSummary(item.itemCode())))
                .toList();
        long total = itemRepository.countByFilter(filter);
        log.info("[ACTION] Complete SearchItems | userId={} | totalCount={}",
                LogMaskingUtil.maskId(query.actorId()),
                total);
        return new PageResult<>(items, PageMeta.of(total, query.page(), query.size(), DEFAULT_SORT));
    }
}
