package com.eprocure.vendor.presentation.response;

import java.util.UUID;

public record RfqLineItemResponse(
        UUID id,
        String itemName,
        String categoryCode,
        String quantity,
        String unit,
        String specifications) {
}
