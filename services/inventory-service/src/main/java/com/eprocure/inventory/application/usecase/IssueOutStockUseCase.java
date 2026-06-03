package com.eprocure.inventory.application.usecase;

import com.eprocure.inventory.application.port.in.IssueOutStockCommand;
import com.eprocure.inventory.application.service.IdempotencyService;
import com.eprocure.inventory.application.service.IssueOutStockResult;
import com.eprocure.inventory.application.service.StockMovementView;
import com.eprocure.inventory.common.exception.BusinessException;
import com.eprocure.inventory.common.exception.ErrorCode;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.eprocure.inventory.domain.model.StockIssueOutRequest;
import com.eprocure.inventory.domain.model.StockMovement;
import com.eprocure.inventory.domain.repository.StockRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IssueOutStockUseCase {
    private static final Logger log = LogManager.getLogger(IssueOutStockUseCase.class);
    private static final String SOURCE_REF_TYPE = "STOCK_ISSUE_OUT";

    private final StockRepository stockRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public IssueOutStockUseCase(
            StockRepository stockRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.stockRepository = stockRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public IssueOutStockResult execute(IssueOutStockCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        UUID key = UUID.fromString(idempotencyKey);
        var replayed = stockRepository.findIssueOutRequestByIdempotencyKey(key);
        if (replayed.isPresent()) {
            log.info("[ACTION] Idempotency hit IssueOutStock | requestId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(replayed.get().id()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return IssueOutStockResult.replayed(findMovementViews(replayed.get().id()));
        }

        if (!stockRepository.existsActiveWarehouse(command.warehouseId())) {
            throw new BusinessException(ErrorCode.INV_002);
        }

        Map<LineKey, BigDecimal> aggregatedLines = aggregateLines(command);
        for (LineKey line : aggregatedLines.keySet()) {
            if (!stockRepository.existsActiveItem(line.itemCode())) {
                throw new BusinessException(ErrorCode.INV_001);
            }
        }

        Instant issuedAt = Instant.now(clock);
        StockIssueOutRequest issueOutRequest = StockIssueOutRequest.create(
                key,
                command.warehouseId(),
                command.prId(),
                command.recipientId(),
                command.actorId(),
                issuedAt,
                command.notes());
        stockRepository.insertIssueOutRequest(issueOutRequest);

        log.info("[ACTION] Start IssueOutStock | requestId={} | warehouseId={} | userId={} | itemCount={}",
                LogMaskingUtil.maskId(issueOutRequest.id()),
                LogMaskingUtil.maskId(command.warehouseId()),
                LogMaskingUtil.maskId(command.actorId()),
                aggregatedLines.size());
        for (Map.Entry<LineKey, BigDecimal> entry : aggregatedLines.entrySet()) {
            LineKey line = entry.getKey();
            BigDecimal quantity = entry.getValue();
            BigDecimal balanceAfter = stockRepository.issueStock(
                            line.itemCode(),
                            command.warehouseId(),
                            quantity,
                            line.unit(),
                            command.actorId(),
                            issuedAt)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INV_003));
            StockMovement movement = StockMovement.issueOut(
                    line.itemCode(),
                    command.warehouseId(),
                    quantity,
                    line.unit(),
                    balanceAfter,
                    issueOutRequest.id(),
                    command.actorId(),
                    issuedAt,
                    command.notes());
            stockRepository.insertStockMovement(movement);
        }

        var movements = findMovementViews(issueOutRequest.id());
        log.info("[ACTION] Complete IssueOutStock | requestId={} | movementsCreated={}",
                LogMaskingUtil.maskId(issueOutRequest.id()),
                movements.size());
        return IssueOutStockResult.fresh(movements);
    }

    private Map<LineKey, BigDecimal> aggregateLines(IssueOutStockCommand command) {
        Map<LineKey, BigDecimal> result = new LinkedHashMap<>();
        for (IssueOutStockCommand.LineItem item : command.items()) {
            LineKey key = new LineKey(item.itemCode(), item.unit());
            result.merge(key, item.quantity(), BigDecimal::add);
        }
        return result;
    }

    private java.util.List<StockMovementView> findMovementViews(UUID issueOutRequestId) {
        return stockRepository.findMovementsBySource(SOURCE_REF_TYPE, issueOutRequestId).stream()
                .map(StockMovementView::from)
                .toList();
    }

    private record LineKey(String itemCode, String unit) {
    }
}
