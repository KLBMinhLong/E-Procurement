package com.eprocure.finance.presentation.response;

import java.util.UUID;

public record InvoiceLineItemResponse(
        int lineNumber,
        UUID poLineItemId,
        String description,
        String quantity,
        String unitPrice,
        String totalPrice) {
}
