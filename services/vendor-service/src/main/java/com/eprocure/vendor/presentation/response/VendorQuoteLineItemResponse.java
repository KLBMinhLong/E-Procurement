package com.eprocure.vendor.presentation.response;

import java.util.UUID;

public record VendorQuoteLineItemResponse(
        UUID rfqLineItemId,
        String itemName,
        String unitPrice,
        String currency,
        String quantity,
        String totalPrice,
        Integer deliveryDays,
        String warranty) {
}
