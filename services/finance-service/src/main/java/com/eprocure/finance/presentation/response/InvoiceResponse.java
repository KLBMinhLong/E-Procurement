package com.eprocure.finance.presentation.response;

import com.eprocure.finance.domain.model.InvoiceStatus;
import com.eprocure.finance.domain.model.MatchStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String invoiceNumber,
        VendorResponse vendor,
        PurchaseOrderResponse po,
        List<InvoiceLineItemResponse> lineItems,
        String subtotal,
        String taxAmount,
        String totalAmount,
        String currency,
        LocalDate invoiceDate,
        LocalDate dueDate,
        InvoiceStatus status,
        MatchResultResponse matchResult,
        Instant createdAt) {

    public record VendorResponse(UUID id, String name) {
    }

    public record PurchaseOrderResponse(UUID id, String poNumber) {
    }

    public record MatchResultResponse(
            MatchStatus poMatchStatus,
            MatchStatus grMatchStatus,
            String qtyVariance,
            String priceVariance,
            Instant matchedAt) {
    }
}
