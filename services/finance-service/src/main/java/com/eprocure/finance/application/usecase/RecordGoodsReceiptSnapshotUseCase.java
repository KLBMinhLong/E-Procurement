package com.eprocure.finance.application.usecase;

import com.eprocure.finance.application.port.in.RecordGoodsReceiptSnapshotCommand;
import com.eprocure.finance.common.util.LogMaskingUtil;
import com.eprocure.finance.domain.model.GoodsReceiptSnapshot;
import com.eprocure.finance.domain.repository.GoodsReceiptSnapshotRepository;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordGoodsReceiptSnapshotUseCase {
    private static final Logger log = LogManager.getLogger(RecordGoodsReceiptSnapshotUseCase.class);
    private static final String HANDLER_NAME = "finance-gr-created-snapshot";

    private final GoodsReceiptSnapshotRepository goodsReceiptSnapshotRepository;

    public RecordGoodsReceiptSnapshotUseCase(GoodsReceiptSnapshotRepository goodsReceiptSnapshotRepository) {
        this.goodsReceiptSnapshotRepository = goodsReceiptSnapshotRepository;
    }

    @Transactional
    public void execute(RecordGoodsReceiptSnapshotCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (goodsReceiptSnapshotRepository.existsProcessedEvent(command.eventId())) {
            log.info("[ACTION] Skip duplicate GoodsReceiptSnapshot | eventId={}", command.eventId());
            return;
        }
        GoodsReceiptSnapshot snapshot = new GoodsReceiptSnapshot(
                command.grId(),
                command.grNumber(),
                command.poId(),
                command.poNumber(),
                command.warehouseId(),
                command.warehouseKeeperId(),
                command.status(),
                command.receivedAt(),
                command.completedAt(),
                command.lineItems(),
                command.eventId());
        log.info("[ACTION] Start RecordGoodsReceiptSnapshot | grId={} | poId={} | lineCount={}",
                LogMaskingUtil.maskId(snapshot.id()),
                LogMaskingUtil.maskId(snapshot.poId()),
                snapshot.lineItems().size());
        goodsReceiptSnapshotRepository.upsert(snapshot);
        goodsReceiptSnapshotRepository.markEventProcessed(
                command.eventId(),
                command.topic(),
                command.partitionId(),
                command.offsetValue(),
                HANDLER_NAME);
        log.info("[ACTION] Complete RecordGoodsReceiptSnapshot | eventId={} | grId={}",
                command.eventId(),
                LogMaskingUtil.maskId(snapshot.id()));
    }
}
