package com.eprocure.pr.presentation.response;

import com.eprocure.pr.domain.model.CatalogItem;
import java.util.UUID;

public record CatalogItemResponse(
        UUID id,
        String itemCode,
        String name,
        String description,
        String categoryCode,
        String unit,
        String unitPrice,
        String quantityOnHand,
        boolean isActive
) {
    public static CatalogItemResponse from(CatalogItem domain) {
        return new CatalogItemResponse(
                domain.getId(),
                domain.getItemCode(),
                domain.getName(),
                domain.getDescription().orElse(null),
                domain.getCategoryCode(),
                domain.getUnit(),
                domain.getUnitPrice().amount().toPlainString(),
                domain.getQuantityOnHand().map(q -> q.toPlainString()).orElse(null),
                domain.isActive()
        );
    }
}
