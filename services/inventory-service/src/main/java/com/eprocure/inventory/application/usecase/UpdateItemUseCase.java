package com.eprocure.inventory.application.usecase;

import com.eprocure.inventory.application.port.in.UpdateItemCommand;
import com.eprocure.inventory.application.service.CatalogItemMutationResult;
import com.eprocure.inventory.application.service.CatalogItemView;
import com.eprocure.inventory.application.service.IdempotencyService;
import com.eprocure.inventory.common.exception.BusinessException;
import com.eprocure.inventory.common.exception.ErrorCode;
import com.eprocure.inventory.common.util.LogMaskingUtil;
import com.eprocure.inventory.domain.model.CatalogItemMutationRequest;
import com.eprocure.inventory.domain.model.Item;
import com.eprocure.inventory.domain.repository.ItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateItemUseCase {
    private static final Logger log = LogManager.getLogger(UpdateItemUseCase.class);
    private static final String OPERATION = "UPDATE";

    private final ItemRepository itemRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public UpdateItemUseCase(
            ItemRepository itemRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.itemRepository = itemRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public CatalogItemMutationResult execute(UpdateItemCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        UUID key = UUID.fromString(idempotencyKey);
        var replayed = itemRepository.findMutationRequestByIdempotencyKey(key);
        if (replayed.isPresent()) {
            log.info("[ACTION] Idempotency hit UpdateItem | itemCode={} | userId={} | key={}",
                    replayed.get().itemCode(),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return CatalogItemMutationResult.replayed(findView(replayed.get().itemCode()));
        }

        Item existing = itemRepository.findByCode(command.itemCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.INV_001));
        Item updated = new Item(
                existing.id(),
                existing.itemCode(),
                command.name() == null ? existing.name() : command.name(),
                command.description() == null ? existing.description() : command.description(),
                existing.categoryCode(),
                existing.unit(),
                command.unitPrice() == null ? existing.unitPrice() : command.unitPrice(),
                command.currency() == null ? existing.currency() : command.currency(),
                command.preferredVendorId() == null ? existing.preferredVendorId() : command.preferredVendorId(),
                command.reorderPoint() == null ? existing.reorderPoint() : command.reorderPoint(),
                command.active() == null ? existing.active() : command.active(),
                existing.createdAt(),
                existing.createdBy(),
                command.actorId());

        log.info("[ACTION] Start UpdateItem | itemCode={} | userId={}",
                updated.itemCode(),
                LogMaskingUtil.maskId(command.actorId()));
        if (!itemRepository.update(updated)) {
            throw new BusinessException(ErrorCode.INV_001);
        }
        itemRepository.insertMutationRequest(CatalogItemMutationRequest.create(
                key,
                OPERATION,
                updated.itemCode(),
                command.actorId(),
                Instant.now(clock)));
        log.info("[ACTION] Complete UpdateItem | itemCode={} | itemId={}",
                updated.itemCode(),
                LogMaskingUtil.maskId(updated.id()));
        return CatalogItemMutationResult.fresh(findView(updated.itemCode()));
    }

    private CatalogItemView findView(String itemCode) {
        var item = itemRepository.findByCode(itemCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INV_001));
        return CatalogItemView.from(item, itemRepository.findStockSummary(item.itemCode()));
    }
}
