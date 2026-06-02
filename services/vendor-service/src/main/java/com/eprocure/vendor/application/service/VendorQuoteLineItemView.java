package com.eprocure.vendor.application.service;

import com.eprocure.vendor.domain.model.VendorQuoteLineItem;
import java.math.BigDecimal;
import java.util.UUID;

public record VendorQuoteLineItemView(
        UUID id,
        UUID rfqLineItemId,
        String itemName,
        BigDecimal unitPrice,
        String currency,
        BigDecimal quantity,
        BigDecimal totalPrice,
        Integer deliveryDays,
        String warranty) {

    public static VendorQuoteLineItemView from(VendorQuoteLineItem item) {
        return new VendorQuoteLineItemView(
                item.id(),
                item.rfqLineItemId(),
                item.itemName(),
                item.unitPrice(),
                item.currency(),
                item.quantity(),
                item.totalPrice(),
                item.deliveryDays(),
                item.warranty());
    }
}
