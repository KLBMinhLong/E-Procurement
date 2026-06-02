package com.eprocure.vendor.domain.event;

import com.eprocure.vendor.domain.model.Rfq;
import com.eprocure.vendor.domain.model.RfqLineItem;
import com.eprocure.vendor.domain.model.Vendor;
import com.eprocure.vendor.domain.model.VendorQuote;
import com.eprocure.vendor.domain.model.VendorQuoteLineItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public record RfqAwardedEvent(
        String eventId,
        String eventType,
        String version,
        String source,
        Instant timestamp,
        UUID traceId,
        Payload payload) {

    public static RfqAwardedEvent create(
            Rfq awardedRfq,
            VendorQuote awardedQuote,
            Vendor awardedVendor,
            UUID actorId,
            Instant timestamp) {
        Objects.requireNonNull(awardedRfq, "awardedRfq must not be null");
        Objects.requireNonNull(awardedQuote, "awardedQuote must not be null");
        Objects.requireNonNull(awardedVendor, "awardedVendor must not be null");
        Map<UUID, RfqLineItem> rfqLineItems = awardedRfq.lineItems().stream()
                .collect(Collectors.toMap(RfqLineItem::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        return new RfqAwardedEvent(
                UUID.randomUUID().toString(),
                "RFQ_AWARDED",
                "1.0",
                "vendor-service",
                timestamp,
                UUID.randomUUID(),
                new Payload(
                        awardedRfq.id(),
                        awardedRfq.rfqNumber(),
                        awardedRfq.prId(),
                        awardedRfq.prNumber(),
                        awardedVendor.id(),
                        awardedVendor.name(),
                        awardedVendor.email(),
                        awardedVendor.taxCode(),
                        awardedQuote.id(),
                        awardedQuote.totalAmount(),
                        awardedQuote.currency(),
                        awardedQuote.validUntil(),
                        awardedQuote.paymentTerms(),
                        awardedRfq.awardReason(),
                        actorId,
                        awardedQuote.lineItems().stream()
                                .map(item -> toLineItem(item, rfqLineItems.get(item.rfqLineItemId())))
                                .toList()));
    }

    public RfqAwardedEvent {
        eventId = requireText(eventId, "eventId");
        eventType = requireText(eventType, "eventType");
        version = requireText(version, "version");
        source = requireText(source, "source");
        timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        traceId = Objects.requireNonNull(traceId, "traceId must not be null");
        payload = Objects.requireNonNull(payload, "payload must not be null");
    }

    public record Payload(
            UUID rfqId,
            String rfqNumber,
            UUID prId,
            String prNumber,
            UUID vendorId,
            String vendorName,
            String vendorEmail,
            String vendorTaxCode,
            UUID awardedQuoteId,
            BigDecimal totalAmount,
            String currency,
            LocalDate validUntil,
            String paymentTerms,
            String awardReason,
            UUID awardedBy,
            List<LineItem> lineItems) {

        public Payload {
            rfqId = Objects.requireNonNull(rfqId, "rfqId must not be null");
            rfqNumber = requireText(rfqNumber, "rfqNumber");
            prId = Objects.requireNonNull(prId, "prId must not be null");
            prNumber = requireText(prNumber, "prNumber");
            vendorId = Objects.requireNonNull(vendorId, "vendorId must not be null");
            vendorName = requireText(vendorName, "vendorName");
            vendorEmail = requireText(vendorEmail, "vendorEmail");
            vendorTaxCode = requireText(vendorTaxCode, "vendorTaxCode");
            awardedQuoteId = Objects.requireNonNull(awardedQuoteId, "awardedQuoteId must not be null");
            totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
            currency = requireText(currency, "currency");
            validUntil = Objects.requireNonNull(validUntil, "validUntil must not be null");
            awardReason = requireText(awardReason, "awardReason");
            awardedBy = Objects.requireNonNull(awardedBy, "awardedBy must not be null");
            lineItems = List.copyOf(lineItems == null ? List.of() : lineItems);
            if (lineItems.isEmpty()) {
                throw new IllegalArgumentException("lineItems must not be empty");
            }
        }
    }

    public record LineItem(
            UUID rfqLineItemId,
            UUID prLineItemId,
            String itemName,
            String categoryCode,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String currency,
            Integer deliveryDays,
            String warranty) {

        public LineItem {
            rfqLineItemId = Objects.requireNonNull(rfqLineItemId, "rfqLineItemId must not be null");
            prLineItemId = Objects.requireNonNull(prLineItemId, "prLineItemId must not be null");
            itemName = requireText(itemName, "itemName");
            categoryCode = requireText(categoryCode, "categoryCode");
            quantity = Objects.requireNonNull(quantity, "quantity must not be null");
            unit = requireText(unit, "unit");
            unitPrice = Objects.requireNonNull(unitPrice, "unitPrice must not be null");
            totalPrice = Objects.requireNonNull(totalPrice, "totalPrice must not be null");
            currency = requireText(currency, "currency");
        }
    }

    private static LineItem toLineItem(VendorQuoteLineItem quoteItem, RfqLineItem rfqItem) {
        Objects.requireNonNull(rfqItem, "rfqItem must not be null");
        return new LineItem(
                quoteItem.rfqLineItemId(),
                rfqItem.prLineItemId(),
                quoteItem.itemName(),
                rfqItem.categoryCode(),
                quoteItem.quantity(),
                rfqItem.unit(),
                quoteItem.unitPrice(),
                quoteItem.totalPrice(),
                quoteItem.currency(),
                quoteItem.deliveryDays(),
                quoteItem.warranty());
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
