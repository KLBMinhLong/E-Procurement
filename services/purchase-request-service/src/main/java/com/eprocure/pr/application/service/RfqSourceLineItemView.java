package com.eprocure.pr.application.service;

import com.eprocure.pr.domain.model.PrLineItem;
import java.math.BigDecimal;
import java.util.UUID;

public record RfqSourceLineItemView(
        UUID id,
        String itemName,
        String categoryCode,
        BigDecimal quantity,
        String unit,
        String specifications) {

    public static RfqSourceLineItemView from(PrLineItem item) {
        return new RfqSourceLineItemView(
                item.getId(),
                item.getItemName(),
                item.getCategoryCode(),
                item.getQuantity().amount(),
                item.getQuantity().unit(),
                item.getSpecifications().orElse(null));
    }
}
