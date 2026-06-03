package com.eprocure.inventory.application.usecase;

import com.eprocure.inventory.application.port.in.CompleteGoodsReceiptCommand;
import com.eprocure.inventory.application.port.out.GrCreatedEventPublisher;
import com.eprocure.inventory.application.service.CompleteGoodsReceiptResult;
import com.eprocure.inventory.application.service.IdempotencyService;
import com.eprocure.inventory.common.exception.BusinessException;
import com.eprocure.inventory.common.exception.ErrorCode;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.eprocure.inventory.domain.event.GrCreatedEvent;
import com.eprocure.inventory.domain.model.GoodsReceipt;
import com.eprocure.inventory.domain.model.GoodsReceiptLineItem;
import com.eprocure.inventory.domain.model.GoodsReceiptStatus;
import com.eprocure.inventory.domain.model.StockMovement;
import com.eprocure.inventory.domain.repository.GoodsReceiptRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompleteGoodsReceiptUseCase {
    private static final Logger log = LogManager.getLogger(CompleteGoodsReceiptUseCase.class);

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final GrCreatedEventPublisher grCreatedEventPublisher;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public CompleteGoodsReceiptUseCase(
            GoodsReceiptRepository goodsReceiptRepository,
            GrCreatedEventPublisher grCreatedEventPublisher,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.grCreatedEventPublisher = grCreatedEventPublisher;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public CompleteGoodsReceiptResult execute(CompleteGoodsReceiptCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        UUID key = UUID.fromString(idempotencyKey);
        var replayed = goodsReceiptRepository.findByIdAndCompleteIdempotencyKey(command.goodsReceiptId(), key);
        if (replayed.isPresent()) {
            log.info("[ACTION] Idempotency hit CompleteGoodsReceipt | grId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(command.goodsReceiptId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return replayResult(replayed.get());
        }

        GoodsReceipt goodsReceipt = goodsReceiptRepository.findById(command.goodsReceiptId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INV_004));
        if (!goodsReceipt.canComplete()) {
            throw new BusinessException(ErrorCode.INV_005);
        }

        log.info("[ACTION] Start CompleteGoodsReceipt | grId={} | grNumber={} | userId={}",
                LogMaskingUtil.maskId(goodsReceipt.id()),
                goodsReceipt.grNumber(),
                LogMaskingUtil.maskId(command.actorId()));

        Instant completedAt = Instant.now(clock);
        Map<UUID, String> resolvedItemCodes = new LinkedHashMap<>();
        Map<String, CompleteGoodsReceiptResult.StockUpdate> updatedStocks = new LinkedHashMap<>();
        int movementsCreated = 0;
        for (GoodsReceiptLineItem lineItem : goodsReceipt.lineItems()) {
            String itemCode = resolveItemCode(lineItem, command.actorId());
            resolvedItemCodes.put(lineItem.id(), itemCode);
            if (lineItem.receivedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal balanceAfter = goodsReceiptRepository.receiveStock(
                    itemCode,
                    goodsReceipt.warehouseId(),
                    lineItem.receivedQuantity(),
                    lineItem.unit(),
                    command.actorId(),
                    completedAt);
            StockMovement movement = StockMovement.receiptIn(
                    itemCode,
                    goodsReceipt.warehouseId(),
                    lineItem.receivedQuantity(),
                    lineItem.unit(),
                    balanceAfter,
                    goodsReceipt.id(),
                    command.actorId(),
                    completedAt,
                    "Receipt from " + goodsReceipt.grNumber());
            goodsReceiptRepository.insertStockMovement(movement);
            updatedStocks.put(itemCode, new CompleteGoodsReceiptResult.StockUpdate(itemCode, balanceAfter));
            movementsCreated++;
        }

        GoodsReceiptStatus completedStatus = goodsReceipt.completionStatus();
        boolean completed = goodsReceiptRepository.markCompleted(goodsReceipt.id(), completedStatus, command.actorId(), completedAt, key);
        if (!completed) {
            throw new BusinessException(ErrorCode.INV_005);
        }
        grCreatedEventPublisher.publish(GrCreatedEvent.create(
                goodsReceipt,
                completedStatus,
                resolvedItemCodes,
                completedAt));

        log.info("[ACTION] Complete CompleteGoodsReceipt | grId={} | grStatus={} | movementsCreated={}",
                LogMaskingUtil.maskId(goodsReceipt.id()),
                completedStatus,
                movementsCreated);
        return CompleteGoodsReceiptResult.fresh(
                completedStatus,
                movementsCreated,
                List.copyOf(updatedStocks.values()));
    }

    private String resolveItemCode(GoodsReceiptLineItem lineItem, UUID actorId) {
        if (lineItem.itemCode() != null && !lineItem.itemCode().isBlank()) {
            return lineItem.itemCode();
        }
        String itemCode = goodsReceiptRepository.findActiveItemCodeForPoLineItem(lineItem.poLineItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INV_001));
        goodsReceiptRepository.updateLineItemCode(lineItem.id(), itemCode, actorId);
        return itemCode;
    }

    private CompleteGoodsReceiptResult replayResult(GoodsReceipt goodsReceipt) {
        List<CompleteGoodsReceiptResult.StockUpdate> updates = goodsReceiptRepository.findStockBalancesByReceipt(goodsReceipt.id()).stream()
                .map(balance -> new CompleteGoodsReceiptResult.StockUpdate(balance.itemCode(), balance.quantityOnHand()))
                .toList();
        return CompleteGoodsReceiptResult.replayed(
                goodsReceipt.status(),
                goodsReceiptRepository.countReceiptMovements(goodsReceipt.id()),
                updates);
    }
}
