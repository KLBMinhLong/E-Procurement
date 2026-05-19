package com.eprocure.pr.application.port.out;

import com.eprocure.pr.domain.model.vo.InventoryCheckResult;
import com.eprocure.pr.domain.model.vo.Quantity;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public interface InventoryCheckPort {
    InventoryCheckResult check(InventoryCheckQuery query);

    record InventoryCheckQuery(
            UUID purchaseRequestId,
            List<LineItem> lineItems) {
        public InventoryCheckQuery {
            purchaseRequestId = Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
            lineItems = lineItems == null ? List.of() : List.copyOf(lineItems);
        }
    }

    record LineItem(
            String itemCode,
            String itemName,
            Quantity quantity,
            boolean fromCatalog) {
        public LineItem {
            itemCode = normalizeOptionalText(itemCode);
            itemName = requireText(itemName, "itemName");
            quantity = Objects.requireNonNull(quantity, "quantity must not be null");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
