package com.eprocure.inventory.presentation.response;

import java.util.UUID;

public record GoodsReceiptLineItemResponse(
        UUID id,
        UUID poLineItemId,
        String itemCode,
        String itemName,
        String orderedQuantity,
        String receivedQuantity,
        String rejectedQuantity,
        String rejectionReason,
        String lotNumber,
        String unit) {
}
