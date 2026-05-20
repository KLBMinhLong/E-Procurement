package com.eprocure.pr.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Command: Update a DRAFT or CHANGES_REQUESTED purchase request.
 */
public record UpdatePurchaseRequestCommand(
        UUID purchaseRequestId,
        UUID actorId,
        String title,
        String justification,
        String priority,
        String urgencyReason,
        LocalDate needByDate,
        List<LineItemCommand> lineItems
) {
    public record LineItemCommand(
            String itemCode,
            String itemName,
            String description,
            String categoryCode,
            BigDecimal quantityAmount,
            String quantityUnit,
            BigDecimal unitPriceAmount,
            String unitPriceCurrency,
            UUID preferredVendorId,
            String specifications,
            String glAccountCode,
            boolean fromCatalog
    ) {}
}
