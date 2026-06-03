package com.eprocure.finance.presentation.response;

public record InvoiceLineItemResponse(
        int lineNumber,
        String description,
        String quantity,
        String unitPrice,
        String totalPrice) {
}
