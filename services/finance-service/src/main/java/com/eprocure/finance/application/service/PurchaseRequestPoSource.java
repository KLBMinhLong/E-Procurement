package com.eprocure.finance.application.service;

import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseRequestPoSource(
        UUID id,
        String prNumber,
        String status,
        UUID requesterId,
        UUID departmentId,
        int fiscalYear,
        LocalDate needByDate,
        Money totalAmount,
        List<LineItem> lineItems) {

    public PurchaseRequestPoSource {
        lineItems = List.copyOf(lineItems == null ? List.of() : lineItems);
    }

    public record LineItem(
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
    }

    public record Quantity(BigDecimal amount, String unit) {
    }
}
