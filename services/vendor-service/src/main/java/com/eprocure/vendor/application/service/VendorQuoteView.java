package com.eprocure.vendor.application.service;

import com.eprocure.vendor.domain.model.VendorQuote;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VendorQuoteView(
        UUID id,
        UUID rfqId,
        UUID vendorId,
        String vendorName,
        List<VendorQuoteLineItemView> lineItems,
        BigDecimal totalAmount,
        String currency,
        LocalDate validUntil,
        String paymentTerms,
        String notes,
        Instant submittedAt,
        BigDecimal evaluationScore,
        String evaluationNote) {

    public static VendorQuoteView from(VendorQuote quote) {
        return new VendorQuoteView(
                quote.id(),
                quote.rfqId(),
                quote.vendorId(),
                quote.vendorName(),
                quote.lineItems().stream().map(VendorQuoteLineItemView::from).toList(),
                quote.totalAmount(),
                quote.currency(),
                quote.validUntil(),
                quote.paymentTerms(),
                quote.notes(),
                quote.submittedAt(),
                quote.evaluationScore(),
                quote.evaluationNote());
    }
}
