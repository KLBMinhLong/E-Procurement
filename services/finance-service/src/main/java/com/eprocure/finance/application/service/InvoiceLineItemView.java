package com.eprocure.finance.application.service;

import com.eprocure.finance.domain.model.InvoiceLineItem;
import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;

public record InvoiceLineItemView(
        int lineNumber,
        String description,
        BigDecimal quantity,
        Money unitPrice,
        Money totalPrice) {

    public static InvoiceLineItemView from(InvoiceLineItem lineItem) {
        return new InvoiceLineItemView(
                lineItem.lineNumber(),
                lineItem.description(),
                lineItem.quantity(),
                lineItem.unitPrice(),
                lineItem.totalPrice());
    }
}
