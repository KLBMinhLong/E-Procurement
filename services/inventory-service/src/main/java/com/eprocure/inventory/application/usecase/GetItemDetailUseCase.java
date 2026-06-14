package com.eprocure.inventory.application.usecase;

import com.eprocure.inventory.application.port.in.GetItemDetailQuery;
import com.eprocure.inventory.application.service.CatalogItemView;
import com.eprocure.inventory.common.exception.BusinessException;
import com.eprocure.inventory.common.exception.ErrorCode;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.eprocure.inventory.domain.repository.ItemRepository;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetItemDetailUseCase {
    private static final Logger log = LogManager.getLogger(GetItemDetailUseCase.class);

    private final ItemRepository itemRepository;

    public GetItemDetailUseCase(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public CatalogItemView execute(GetItemDetailQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        log.info("[ACTION] Start GetItemDetail | userId={} | itemCode={}",
                LogMaskingUtil.maskId(query.actorId()),
                query.itemCode());
        var item = itemRepository.findByCode(query.itemCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.INV_001));
        log.info("[ACTION] Complete GetItemDetail | userId={} | itemCode={}",
                LogMaskingUtil.maskId(query.actorId()),
                item.itemCode());
        return CatalogItemView.from(item, itemRepository.findStockSummary(item.itemCode()));
    }
}
