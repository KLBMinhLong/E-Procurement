package com.eprocure.inventory.application.service;

import com.eprocure.inventory.domain.model.GoodsReceiptLineItem;
import java.math.BigDecimal;
import java.util.UUID;

public record GoodsReceiptLineItemView(
        UUID id,
        UUID poLineItemId,
        String itemCode,
        String itemName,
        BigDecimal orderedQuantity,
        BigDecimal receivedQuantity,
        BigDecimal rejectedQuantity,
        String rejectionReason,
        String lotNumber,
        String unit) {

    public static GoodsReceiptLineItemView from(GoodsReceiptLineItem lineItem) {
        return new GoodsReceiptLineItemView(
                lineItem.id(),
                lineItem.poLineItemId(),
                lineItem.itemCode(),
                lineItem.itemName(),
                lineItem.orderedQuantity(),
                lineItem.receivedQuantity(),
                lineItem.rejectedQuantity(),
                lineItem.rejectionReason(),
                lineItem.lotNumber(),
                lineItem.unit());
    }
}
