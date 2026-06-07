package com.eprocure.pr.application.service;

import com.eprocure.pr.domain.model.PrLineItem;
import com.eprocure.pr.domain.model.vo.Money;
import com.eprocure.pr.domain.model.vo.Quantity;
import java.util.UUID;

public record PoSourceLineItemView(
        UUID id,
        int lineNumber,
        String itemCode,
        String itemName,
        String description,
        String categoryCode,
        Quantity quantity,
        Money unitPrice,
        Money totalPrice,
        UUID preferredVendorId,
        String specifications,
        String glAccountCode,
        boolean fromCatalog) {

    public static PoSourceLineItemView from(PrLineItem item) {
        return new PoSourceLineItemView(
                item.getId(),
                item.getLineNumber(),
                item.getItemCode().orElse(null),
                item.getItemName(),
                item.getDescription().orElse(null),
                item.getCategoryCode(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice(),
                item.getPreferredVendorId().orElse(null),
                item.getSpecifications().orElse(null),
                item.getGlAccountCode(),
                item.isFromCatalog());
    }
}
