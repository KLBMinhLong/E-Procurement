package com.eprocure.inventory.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record GetGoodsReceiptQuery(UUID actorId, UUID goodsReceiptId) {
    public GetGoodsReceiptQuery {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        goodsReceiptId = Objects.requireNonNull(goodsReceiptId, "goodsReceiptId must not be null");
    }
}
