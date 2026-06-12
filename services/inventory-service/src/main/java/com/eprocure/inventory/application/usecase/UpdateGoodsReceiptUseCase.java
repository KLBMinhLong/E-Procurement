package com.eprocure.inventory.application.usecase;

import com.eprocure.inventory.application.port.in.UpdateGoodsReceiptCommand;
import com.eprocure.inventory.application.service.GoodsReceiptMutationResult;
import com.eprocure.inventory.application.service.GoodsReceiptView;
import com.eprocure.inventory.application.service.IdempotencyService;
import com.eprocure.inventory.common.exception.BusinessException;
import com.eprocure.inventory.common.exception.ErrorCode;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.eprocure.inventory.domain.model.GoodsReceipt;
import com.eprocure.inventory.domain.model.GoodsReceiptLineItem;
import com.eprocure.inventory.domain.model.GoodsReceiptStatus;
import com.eprocure.inventory.domain.model.PurchaseOrderSnapshot;
import com.eprocure.inventory.domain.repository.GoodsReceiptRepository;
import com.eprocure.inventory.domain.repository.PurchaseOrderSnapshotRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
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
public class UpdateGoodsReceiptUseCase {
    private static final Logger log = LogManager.getLogger(UpdateGoodsReceiptUseCase.class);

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final PurchaseOrderSnapshotRepository purchaseOrderSnapshotRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;
    private final BigDecimal toleranceMultiplier;

    public UpdateGoodsReceiptUseCase(
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
    public GoodsReceiptMutationResult execute(UpdateGoodsReceiptCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        UUID key = UUID.fromString(idempotencyKey);

        var replayed = goodsReceiptRepository.findByIdAndUpdateIdempotencyKey(command.goodsReceiptId(), key);
        if (replayed.isPresent()) {
            log.info("[ACTION] Idempotency hit UpdateGoodsReceipt | grId={} | userId={} | key={}",
                    LogMaskingUtil.maskId(command.goodsReceiptId()),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return GoodsReceiptMutationResult.replayed(GoodsReceiptView.from(replayed.get()));
        }

        GoodsReceipt existing = goodsReceiptRepository.findById(command.goodsReceiptId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INV_004));
        if (existing.status() != GoodsReceiptStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INV_005);
        }

        PurchaseOrderSnapshot snapshot = purchaseOrderSnapshotRepository.findByPoId(existing.poId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INV_009));
        GoodsReceipt updated = new GoodsReceipt(
                existing.id(),
                existing.grNumber(),
                existing.poId(),
                existing.poNumber(),
                existing.warehouseId(),
                existing.warehouseName(),
                existing.warehouseKeeperId(),
                existing.warehouseKeeperFullName(),
                command.receivedAt() == null ? existing.receivedAt() : command.receivedAt(),
                existing.status(),
                toLineItems(command, snapshot, existing),
                command.notes(),
                existing.createdAt(),
                existing.createdBy(),
                command.actorId(),
                existing.idempotencyKey());

        log.info("[ACTION] Start UpdateGoodsReceipt | grId={} | grNumber={} | userId={} | lineCount={}",
                LogMaskingUtil.maskId(updated.id()),
                updated.grNumber(),
                LogMaskingUtil.maskId(command.actorId()),
                updated.lineItems().size());
        boolean saved = goodsReceiptRepository.updateDraft(updated, command.actorId(), Instant.now(clock), key);
        if (!saved) {
            throw new BusinessException(ErrorCode.INV_005);
        }
        GoodsReceipt persisted = goodsReceiptRepository.findById(updated.id()).orElse(updated);
        log.info("[ACTION] Complete UpdateGoodsReceipt | grId={} | grNumber={}",
                LogMaskingUtil.maskId(persisted.id()),
                persisted.grNumber());
        return GoodsReceiptMutationResult.fresh(GoodsReceiptView.from(persisted));
    }

    private java.util.List<GoodsReceiptLineItem> toLineItems(
            UpdateGoodsReceiptCommand command,
            PurchaseOrderSnapshot snapshot,
            GoodsReceipt existing) {
        Map<UUID, PurchaseOrderSnapshot.LineItem> poLines = snapshot.lineItems().stream()
                .collect(Collectors.toMap(PurchaseOrderSnapshot.LineItem::poLineItemId, Function.identity()));
        Map<UUID, GoodsReceiptLineItem> existingLines = new HashMap<>();
        for (GoodsReceiptLineItem lineItem : existing.lineItems()) {
            existingLines.put(lineItem.poLineItemId(), lineItem);
        }
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
                    GoodsReceiptLineItem existingLine = existingLines.get(item.poLineItemId());
                    return new GoodsReceiptLineItem(
                            existingLine == null ? UUID.randomUUID() : existingLine.id(),
                            poLine.poLineItemId(),
                            existingLine == null ? null : existingLine.itemCode(),
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

    private void ensureWithinTolerance(UpdateGoodsReceiptCommand.LineItem item, PurchaseOrderSnapshot.LineItem poLine) {
        BigDecimal maximum = poLine.quantity().multiply(toleranceMultiplier);
        if (item.inspectedQuantity().compareTo(maximum) > 0) {
            throw new BusinessException(ErrorCode.INV_006);
        }
    }
}
