package com.eprocure.inventory.application.usecase;

import com.eprocure.inventory.application.port.in.CreateGoodsReceiptCommand;
import com.eprocure.inventory.application.service.GoodsReceiptMutationResult;
import com.eprocure.inventory.application.service.GoodsReceiptView;
import com.eprocure.inventory.application.service.IdempotencyService;
import com.eprocure.inventory.common.exception.BusinessException;
import com.eprocure.inventory.common.exception.ErrorCode;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.eprocure.inventory.domain.model.GoodsReceipt;
import com.eprocure.inventory.domain.model.GoodsReceiptLineItem;
import com.eprocure.inventory.domain.model.PurchaseOrderSnapshot;
import com.eprocure.inventory.domain.repository.GoodsReceiptRepository;
import com.eprocure.inventory.domain.repository.PurchaseOrderSnapshotRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateGoodsReceiptUseCase {
    private static final Logger log = LogManager.getLogger(CreateGoodsReceiptUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "goods-receipt-create";

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final PurchaseOrderSnapshotRepository purchaseOrderSnapshotRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;
    private final BigDecimal toleranceMultiplier;

    public CreateGoodsReceiptUseCase(
            GoodsReceiptRepository goodsReceiptRepository,
            PurchaseOrderSnapshotRepository purchaseOrderSnapshotRepository,
            IdempotencyService idempotencyService,
            Clock clock,
            @Value("${eprocure.inventory.gr-quantity-tolerance-pct:10}") BigDecimal tolerancePct) {
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.purchaseOrderSnapshotRepository = purchaseOrderSnapshotRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
        this.toleranceMultiplier = BigDecimal.ONE.add(tolerancePct.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
    }

    @Transactional
    public GoodsReceiptMutationResult execute(CreateGoodsReceiptCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        UUID key = UUID.fromString(idempotencyKey);
        var existing = goodsReceiptRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            log.info("[ACTION] Idempotency hit CreateGoodsReceipt | poId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(command.poId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return GoodsReceiptMutationResult.replayed(GoodsReceiptView.from(existing.get()));
        }

        PurchaseOrderSnapshot snapshot = purchaseOrderSnapshotRepository.findByPoId(command.poId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INV_009));
        GoodsReceiptRepository.WarehouseSnapshot warehouse = goodsReceiptRepository.findActiveWarehouseById(command.warehouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INV_002));

        Instant now = Instant.now(clock);
        GoodsReceipt goodsReceipt = GoodsReceipt.create(
                UUID.randomUUID(),
                goodsReceiptRepository.nextGrNumber(now.atZone(ZoneOffset.UTC).getYear()),
                snapshot.poId(),
                snapshot.poNumber(),
                warehouse.id(),
                warehouse.name(),
                command.actorId(),
                command.actorFullName(),
                command.receivedAt(),
                toLineItems(command, snapshot),
                command.notes(),
                now,
                key);

        log.info("[ACTION] Start CreateGoodsReceipt | poId={} | warehouseId={} | userId={} | lineCount={}",
                LogMaskingUtil.maskId(command.poId()),
                LogMaskingUtil.maskId(command.warehouseId()),
                LogMaskingUtil.maskId(command.actorId()),
                goodsReceipt.lineItems().size());
        goodsReceiptRepository.insert(goodsReceipt);
        GoodsReceipt persisted = goodsReceiptRepository.findById(goodsReceipt.id()).orElse(goodsReceipt);
        log.info("[ACTION] Complete CreateGoodsReceipt | grId={} | grNumber={}",
                LogMaskingUtil.maskId(persisted.id()),
                persisted.grNumber());
        return GoodsReceiptMutationResult.fresh(GoodsReceiptView.from(persisted));
    }

    private java.util.List<GoodsReceiptLineItem> toLineItems(
            CreateGoodsReceiptCommand command,
            PurchaseOrderSnapshot snapshot) {
        Map<UUID, PurchaseOrderSnapshot.LineItem> poLines = snapshot.lineItems().stream()
                .collect(Collectors.toMap(PurchaseOrderSnapshot.LineItem::poLineItemId, Function.identity()));
        HashSet<UUID> seenLineIds = new HashSet<>();
        return command.lineItems().stream()
                .map(item -> {
                    if (!seenLineIds.add(item.poLineItemId())) {
                        throw new BusinessException(ErrorCode.INV_010);
                    }
                    PurchaseOrderSnapshot.LineItem poLine = poLines.get(item.poLineItemId());
                    if (poLine == null) {
                        throw new BusinessException(ErrorCode.INV_010);
                    }
                    ensureWithinTolerance(item, poLine);
                    return new GoodsReceiptLineItem(
                            UUID.randomUUID(),
                            poLine.poLineItemId(),
                            null,
                            poLine.itemName(),
                            poLine.quantity(),
                            item.receivedQuantity(),
                            item.rejectedQuantity(),
                            poLine.unit(),
                            item.rejectionReason(),
                            item.lotNumber());
                })
                .toList();
    }

    private void ensureWithinTolerance(CreateGoodsReceiptCommand.LineItem item, PurchaseOrderSnapshot.LineItem poLine) {
        BigDecimal maximum = poLine.quantity().multiply(toleranceMultiplier);
        if (item.inspectedQuantity().compareTo(maximum) > 0) {
            throw new BusinessException(ErrorCode.INV_006);
        }
    }
}
