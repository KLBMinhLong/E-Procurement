package com.eprocure.inventory.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record CompleteGoodsReceiptCommand(UUID actorId, UUID goodsReceiptId) {

    public CompleteGoodsReceiptCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        goodsReceiptId = Objects.requireNonNull(goodsReceiptId, "goodsReceiptId must not be null");
    }
}
