package com.eprocure.inventory.application.usecase;

import com.eprocure.inventory.application.port.in.AdjustStockCommand;
import com.eprocure.inventory.application.service.IdempotencyService;
import com.eprocure.inventory.application.service.StockAdjustmentResult;
import com.eprocure.inventory.application.service.StockMovementView;
import com.eprocure.inventory.common.exception.BusinessException;
import com.eprocure.inventory.common.exception.ErrorCode;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.eprocure.inventory.domain.model.StockAdjustmentRequest;
import com.eprocure.inventory.domain.model.StockMovement;
import com.eprocure.inventory.domain.repository.StockRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdjustStockUseCase {
    private static final Logger log = LogManager.getLogger(AdjustStockUseCase.class);
    private static final String SOURCE_REF_TYPE = "STOCK_ADJUSTMENT";

    private final StockRepository stockRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public AdjustStockUseCase(
            StockRepository stockRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.stockRepository = stockRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public StockAdjustmentResult execute(AdjustStockCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        UUID key = UUID.fromString(idempotencyKey);

        var replayed = stockRepository.findAdjustmentRequestByIdempotencyKey(key);
        if (replayed.isPresent()) {
            log.info("[ACTION] Idempotency hit AdjustStock | requestId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(replayed.get().id()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return StockAdjustmentResult.replayed(findMovementView(replayed.get().id()));
        }

        if (!stockRepository.existsActiveWarehouse(command.warehouseId())) {
            throw new BusinessException(ErrorCode.INV_002);
        }
        if (!stockRepository.existsActiveItem(command.itemCode())) {
            throw new BusinessException(ErrorCode.INV_001);
        }

        BigDecimal previousQuantity = stockRepository.findStockQuantity(
                        command.itemCode(),
                        command.warehouseId(),
                        command.unit())
                .orElse(BigDecimal.ZERO);
        BigDecimal delta = command.newQuantity().subtract(previousQuantity);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(ErrorCode.INV_011);
        }

        Instant adjustedAt = Instant.now(clock);
        StockAdjustmentRequest adjustmentRequest = StockAdjustmentRequest.create(
                key,
                command.warehouseId(),
                command.itemCode(),
                previousQuantity,
                command.newQuantity(),
                command.unit(),
                command.actorId(),
                adjustedAt,
                command.reason());
        stockRepository.insertAdjustmentRequest(adjustmentRequest);

        log.info("[ACTION] Start AdjustStock | requestId={} | itemCode={} | warehouseId={} | userId={}",
                LogMaskingUtil.maskId(adjustmentRequest.id()),
                command.itemCode(),
                LogMaskingUtil.maskId(command.warehouseId()),
                LogMaskingUtil.maskId(command.actorId()));

        BigDecimal balanceAfter = stockRepository.adjustStock(
                command.itemCode(),
                command.warehouseId(),
                command.newQuantity(),
                command.unit(),
                command.actorId(),
                adjustedAt);
        StockMovement movement = StockMovement.adjustment(
                command.itemCode(),
                command.warehouseId(),
                delta,
                command.unit(),
                balanceAfter,
                adjustmentRequest.id(),
                command.actorId(),
                adjustedAt,
                command.reason());
        stockRepository.insertStockMovement(movement);

        StockMovementView view = findMovementView(adjustmentRequest.id());
        log.info("[ACTION] Complete AdjustStock | requestId={} | delta={}",
                LogMaskingUtil.maskId(adjustmentRequest.id()),
                delta.toPlainString());
        return StockAdjustmentResult.fresh(view);
    }

    private StockMovementView findMovementView(UUID adjustmentRequestId) {
        return stockRepository.findMovementsBySource(SOURCE_REF_TYPE, adjustmentRequestId).stream()
                .findFirst()
                .map(StockMovementView::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.SYS_001));
    }
}
