package com.eprocure.inventory.application.usecase;

import com.eprocure.inventory.application.port.in.ListGoodsReceiptsQuery;
import com.eprocure.inventory.application.service.GoodsReceiptView;
import com.eprocure.inventory.application.service.PageMeta;
import com.eprocure.inventory.application.service.PageResult;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.eprocure.inventory.domain.repository.GoodsReceiptFilter;
import com.eprocure.inventory.domain.repository.GoodsReceiptRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListGoodsReceiptsUseCase {
    private static final Logger log = LogManager.getLogger(ListGoodsReceiptsUseCase.class);
    private static final String DEFAULT_SORT = "createdAt,desc";

    private final GoodsReceiptRepository goodsReceiptRepository;

    public ListGoodsReceiptsUseCase(GoodsReceiptRepository goodsReceiptRepository) {
        this.goodsReceiptRepository = goodsReceiptRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<GoodsReceiptView> execute(ListGoodsReceiptsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        GoodsReceiptFilter filter = new GoodsReceiptFilter(
                query.status(),
                query.poId(),
                query.warehouseId(),
                toStartInstant(query.fromDate()),
                toEndExclusiveInstant(query.toDate()),
                query.page(),
                query.size(),
                (query.page() - 1) * query.size());

        log.info("[ACTION] Start ListGoodsReceipts | userId={} | page={} | size={}",
                LogMaskingUtil.maskId(query.actorId()),
                query.page(),
                query.size());
        var items = goodsReceiptRepository.findByFilter(filter).stream()
                .map(GoodsReceiptView::from)
                .toList();
        long total = goodsReceiptRepository.countByFilter(filter);
        log.info("[ACTION] Complete ListGoodsReceipts | userId={} | totalCount={}",
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
