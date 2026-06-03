package com.eprocure.finance.application.service;

import com.eprocure.finance.domain.model.InvoiceLineItem;
import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceLineItemView(
        int lineNumber,
        UUID poLineItemId,
        String description,
        BigDecimal quantity,
        Money unitPrice,
        Money totalPrice) {

    public static InvoiceLineItemView from(InvoiceLineItem lineItem) {
        return new InvoiceLineItemView(
                lineItem.lineNumber(),
                lineItem.poLineItemId(),
                lineItem.description(),
                lineItem.quantity(),
                lineItem.unitPrice(),
                lineItem.totalPrice());
    }
}
