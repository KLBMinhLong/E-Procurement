package com.eprocure.pr.presentation.response;

import com.eprocure.pr.domain.model.CatalogCategory;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public record CatalogCategoryResponse(
        String code,
        String name,
        String parentCode,
        String requiresRfqAbove,
        boolean isCapex,
        List<CatalogCategoryResponse> children
) {
    public static CatalogCategoryResponse from(CatalogCategory domain) {
        String requiresRfqAboveStr = domain.getRequiresRfqAbove()
                .map(money -> money.amount().toPlainString())
                .orElse(null);

        List<CatalogCategoryResponse> childrenResp = domain.getChildren().stream()
                .map(CatalogCategoryResponse::from)
                .collect(Collectors.toList());

        return new CatalogCategoryResponse(
                domain.getCode(),
                domain.getName(),
                domain.getParentCode().orElse(null),
                requiresRfqAboveStr,
                domain.isCapex(),
                childrenResp.isEmpty() ? null : childrenResp
        );
    }
}
