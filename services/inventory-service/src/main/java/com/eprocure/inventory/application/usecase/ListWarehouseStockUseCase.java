package com.eprocure.inventory.application.usecase;

import com.eprocure.inventory.application.port.in.ListWarehouseStockQuery;
import com.eprocure.inventory.application.service.PageMeta;
import com.eprocure.inventory.application.service.PageResult;
import com.eprocure.inventory.application.service.StockEntryView;
import com.eprocure.inventory.common.exception.BusinessException;
import com.eprocure.inventory.common.exception.ErrorCode;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.eprocure.inventory.domain.repository.StockEntryFilter;
import com.eprocure.inventory.domain.repository.StockRepository;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListWarehouseStockUseCase {
    private static final Logger log = LogManager.getLogger(ListWarehouseStockUseCase.class);
    private static final String DEFAULT_SORT = "itemName,asc";

    private final StockRepository stockRepository;

    public ListWarehouseStockUseCase(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<StockEntryView> execute(ListWarehouseStockQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        if (!stockRepository.existsActiveWarehouse(query.warehouseId())) {
            throw new BusinessException(ErrorCode.INV_002);
        }

        StockEntryFilter filter = new StockEntryFilter(
                null,
                query.warehouseId(),
                query.belowReorder(),
                query.page(),
                query.size(),
                (query.page() - 1) * query.size());
        log.info("[ACTION] Start ListWarehouseStock | userId={} | warehouseId={} | page={} | size={}",
                LogMaskingUtil.maskId(query.actorId()),
                LogMaskingUtil.maskId(query.warehouseId()),
                query.page(),
                query.size());
        var items = stockRepository.findStockEntries(filter).stream()
                .map(StockEntryView::from)
                .toList();
        long total = stockRepository.countStockEntries(filter);
        log.info("[ACTION] Complete ListWarehouseStock | userId={} | warehouseId={} | totalCount={}",
                LogMaskingUtil.maskId(query.actorId()),
                LogMaskingUtil.maskId(query.warehouseId()),
                total);
        return new PageResult<>(items, PageMeta.of(total, query.page(), query.size(), DEFAULT_SORT));
    }
}
