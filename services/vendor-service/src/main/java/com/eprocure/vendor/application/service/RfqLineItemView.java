package com.eprocure.vendor.application.service;

import com.eprocure.vendor.domain.model.RfqLineItem;
import java.math.BigDecimal;
import java.util.UUID;

public record RfqLineItemView(
        UUID id,
        String itemName,
        String categoryCode,
        BigDecimal quantity,
        String unit,
        String specifications) {

    public static RfqLineItemView from(RfqLineItem item) {
        return new RfqLineItemView(
                item.id(),
                item.itemName(),
                item.categoryCode(),
                item.quantity(),
                item.unit(),
                item.specifications());
    }
}
