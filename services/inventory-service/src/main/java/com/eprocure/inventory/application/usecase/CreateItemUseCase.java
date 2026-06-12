package com.eprocure.inventory.application.usecase;

import com.eprocure.inventory.application.port.in.CreateItemCommand;
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
public class CreateItemUseCase {
    private static final Logger log = LogManager.getLogger(CreateItemUseCase.class);
    private static final String OPERATION = "CREATE";

    private final ItemRepository itemRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public CreateItemUseCase(
            ItemRepository itemRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.itemRepository = itemRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public CatalogItemMutationResult execute(CreateItemCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyService.verify(idempotencyKey);
        UUID key = UUID.fromString(idempotencyKey);
        var replayed = itemRepository.findMutationRequestByIdempotencyKey(key);
        if (replayed.isPresent()) {
            log.info("[ACTION] Idempotency hit CreateItem | itemCode={} | userId={} | key={}",
                    replayed.get().itemCode(),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return CatalogItemMutationResult.replayed(findView(replayed.get().itemCode()));
        }

        if (itemRepository.existsActiveCode(command.itemCode())) {
            throw new BusinessException(ErrorCode.INV_007);
        }

        Instant now = Instant.now(clock);
        Item item = Item.create(
                command.itemCode(),
                command.name(),
                command.description(),
                command.categoryCode(),
                command.unit(),
                command.unitPrice(),
                command.currency(),
                command.preferredVendorId(),
                command.reorderPoint(),
                command.actorId(),
                now);
        log.info("[ACTION] Start CreateItem | itemCode={} | userId={}",
                item.itemCode(),
                LogMaskingUtil.maskId(command.actorId()));
        itemRepository.insert(item);
        itemRepository.insertMutationRequest(CatalogItemMutationRequest.create(
                key,
                OPERATION,
                item.itemCode(),
                command.actorId(),
                now));
        log.info("[ACTION] Complete CreateItem | itemCode={} | itemId={}",
                item.itemCode(),
                LogMaskingUtil.maskId(item.id()));
        return CatalogItemMutationResult.fresh(findView(item.itemCode()));
    }

    private CatalogItemView findView(String itemCode) {
        var item = itemRepository.findByCode(itemCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INV_001));
        return CatalogItemView.from(item, itemRepository.findStockSummary(item.itemCode()));
    }
}
