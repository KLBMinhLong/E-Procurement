package com.eprocure.vendor.presentation.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VendorQuoteResponse(
        UUID id,
        UUID rfqId,
        UUID vendorId,
        String vendorName,
        List<VendorQuoteLineItemResponse> lineItems,
        String totalAmount,
        String currency,
        LocalDate validUntil,
        String paymentTerms,
        String notes,
        Instant submittedAt,
        BigDecimal evaluationScore,
        String evaluationNote) {
}
