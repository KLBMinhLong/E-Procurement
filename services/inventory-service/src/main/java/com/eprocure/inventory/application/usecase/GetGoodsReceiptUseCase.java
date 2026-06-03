package com.eprocure.inventory.application.usecase;

import com.eprocure.inventory.application.port.in.GetGoodsReceiptQuery;
import com.eprocure.inventory.application.service.GoodsReceiptView;
import com.eprocure.inventory.common.exception.BusinessException;
import com.eprocure.inventory.common.exception.ErrorCode;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.eprocure.inventory.domain.repository.GoodsReceiptRepository;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetGoodsReceiptUseCase {
    private static final Logger log = LogManager.getLogger(GetGoodsReceiptUseCase.class);

    private final GoodsReceiptRepository goodsReceiptRepository;

    public GetGoodsReceiptUseCase(GoodsReceiptRepository goodsReceiptRepository) {
        this.goodsReceiptRepository = goodsReceiptRepository;
    }

    @Transactional(readOnly = true)
    public GoodsReceiptView execute(GetGoodsReceiptQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        log.info("[ACTION] Start GetGoodsReceipt | userId={} | grId={}",
                LogMaskingUtil.maskId(query.actorId()),
                LogMaskingUtil.maskId(query.goodsReceiptId()));
        return goodsReceiptRepository.findById(query.goodsReceiptId())
                .map(GoodsReceiptView::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.INV_004));
    }
}
