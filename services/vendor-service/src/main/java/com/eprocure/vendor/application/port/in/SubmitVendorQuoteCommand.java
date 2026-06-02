package com.eprocure.vendor.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SubmitVendorQuoteCommand(
        UUID actorId,
        UUID rfqId,
        UUID vendorId,
        String currency,
        LocalDate validUntil,
        List<SubmitVendorQuoteLineItemCommand> lineItems,
        String paymentTerms,
        String notes) {

    public SubmitVendorQuoteCommand {
        Objects.requireNonNull(actorId, "actorId must not be null");
        Objects.requireNonNull(rfqId, "rfqId must not be null");
        Objects.requireNonNull(vendorId, "vendorId must not be null");
        lineItems = List.copyOf(lineItems == null ? List.of() : lineItems);
    }

    public record SubmitVendorQuoteLineItemCommand(
            UUID rfqLineItemId,
            BigDecimal unitPrice,
            Integer deliveryDays,
            String warranty) {

        public SubmitVendorQuoteLineItemCommand {
            Objects.requireNonNull(rfqLineItemId, "rfqLineItemId must not be null");
        }
    }
}
