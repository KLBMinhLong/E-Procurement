package com.eprocure.finance.application.service;

import com.eprocure.finance.domain.model.PurchaseOrderLineItem;
import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseOrderLineItemView(
        UUID id,
        int lineNumber,
        UUID prLineItemId,
        String itemName,
        String categoryCode,
        BigDecimal quantity,
        String unit,
        Money unitPrice,
        Money totalPrice) {

    public static PurchaseOrderLineItemView from(PurchaseOrderLineItem lineItem) {
        return new PurchaseOrderLineItemView(
                lineItem.id(),
                lineItem.lineNumber(),
                lineItem.prLineItemId(),
                lineItem.itemName(),
                lineItem.categoryCode(),
                lineItem.quantity(),
                lineItem.unit(),
                lineItem.unitPrice(),
                lineItem.totalPrice());
    }
}
