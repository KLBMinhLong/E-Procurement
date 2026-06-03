package com.eprocure.inventory.application.usecase;

import com.eprocure.inventory.application.port.in.ListStockMovementsQuery;
import com.eprocure.inventory.application.service.PageMeta;
import com.eprocure.inventory.application.service.PageResult;
import com.eprocure.inventory.application.service.StockMovementView;
import com.eprocure.inventory.common.exception.BusinessException;
import com.eprocure.inventory.common.exception.ErrorCode;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.eprocure.inventory.domain.repository.StockMovementFilter;
import com.eprocure.inventory.domain.repository.StockRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListStockMovementsUseCase {
    private static final Logger log = LogManager.getLogger(ListStockMovementsUseCase.class);
    private static final String DEFAULT_SORT = "performedAt,desc";

    private final StockRepository stockRepository;

    public ListStockMovementsUseCase(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<StockMovementView> execute(ListStockMovementsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        if (query.itemCode() != null && !stockRepository.existsActiveItem(query.itemCode())) {
            throw new BusinessException(ErrorCode.INV_001);
        }
        if (query.warehouseId() != null && !stockRepository.existsActiveWarehouse(query.warehouseId())) {
            throw new BusinessException(ErrorCode.INV_002);
        }

        StockMovementFilter filter = new StockMovementFilter(
                query.itemCode(),
                query.warehouseId(),
                query.movementType(),
                toStartInstant(query.fromDate()),
                toEndExclusiveInstant(query.toDate()),
                query.page(),
                query.size(),
                (query.page() - 1) * query.size());
        log.info("[ACTION] Start ListStockMovements | userId={} | page={} | size={}",
                LogMaskingUtil.maskId(query.actorId()),
                query.page(),
                query.size());
        var items = stockRepository.findMovements(filter).stream()
                .map(StockMovementView::from)
                .toList();
        long total = stockRepository.countMovements(filter);
        log.info("[ACTION] Complete ListStockMovements | userId={} | totalCount={}",
                LogMaskingUtil.maskId(query.actorId()),
                total);
        return new PageResult<>(items, PageMeta.of(total, query.page(), query.size(), DEFAULT_SORT));
    }

    private Instant toStartInstant(LocalDate date) {
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant toEndExclusiveInstant(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
