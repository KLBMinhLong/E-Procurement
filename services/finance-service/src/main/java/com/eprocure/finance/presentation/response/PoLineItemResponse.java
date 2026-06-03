package com.eprocure.finance.presentation.response;

import java.util.UUID;

public record PoLineItemResponse(
        UUID id,
        int lineNumber,
        UUID prLineItemId,
        String itemName,
        String categoryCode,
        QuantityResponse quantity,
        String unitPrice,
        String totalPrice,
        String currency) {
}
