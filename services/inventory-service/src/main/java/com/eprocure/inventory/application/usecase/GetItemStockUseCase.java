package com.eprocure.inventory.application.usecase;

import com.eprocure.inventory.application.port.in.GetItemStockQuery;
import com.eprocure.inventory.application.service.StockEntryView;
import com.eprocure.inventory.common.exception.BusinessException;
import com.eprocure.inventory.common.exception.ErrorCode;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.eprocure.inventory.domain.repository.StockEntryFilter;
import com.eprocure.inventory.domain.repository.StockRepository;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetItemStockUseCase {
    private static final Logger log = LogManager.getLogger(GetItemStockUseCase.class);
    private static final int MAX_WAREHOUSE_ROWS = 1000;

    private final StockRepository stockRepository;

    public GetItemStockUseCase(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional(readOnly = true)
    public List<StockEntryView> execute(GetItemStockQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        log.info("[ACTION] Start GetItemStock | userId={} | itemCode={} | warehouseId={}",
                LogMaskingUtil.maskId(query.actorId()),
                query.itemCode(),
                LogMaskingUtil.maskId(query.warehouseId()));
        if (!stockRepository.existsActiveItem(query.itemCode())) {
            throw new BusinessException(ErrorCode.INV_001);
        }
        if (query.warehouseId() != null && !stockRepository.existsActiveWarehouse(query.warehouseId())) {
            throw new BusinessException(ErrorCode.INV_002);
        }

        StockEntryFilter filter = new StockEntryFilter(
                query.itemCode(),
                query.warehouseId(),
                null,
                1,
                MAX_WAREHOUSE_ROWS,
                0);
        List<StockEntryView> result = stockRepository.findStockEntries(filter).stream()
                .map(StockEntryView::from)
                .toList();
        log.info("[ACTION] Complete GetItemStock | userId={} | itemCode={} | rows={}",
                LogMaskingUtil.maskId(query.actorId()),
                query.itemCode(),
                result.size());
        return result;
    }
}
