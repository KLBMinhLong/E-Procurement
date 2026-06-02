package com.eprocure.vendor.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PurchaseRequestRfqSourcePort {
    PurchaseRequestRfqSource getSource(UUID purchaseRequestId);

    record PurchaseRequestRfqSource(
            UUID id,
            String prNumber,
            String status,
            List<PurchaseRequestRfqLineItem> lineItems) {

        public PurchaseRequestRfqSource {
            lineItems = List.copyOf(lineItems == null ? List.of() : lineItems);
        }
    }

    record PurchaseRequestRfqLineItem(
            UUID id,
            String itemName,
            String categoryCode,
            BigDecimal quantity,
            String unit,
            String specifications) {
    }
}
