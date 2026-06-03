package com.eprocure.finance.domain.event;

import com.eprocure.finance.domain.model.PurchaseOrder;
import com.eprocure.finance.domain.model.PurchaseOrderLineItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PurchaseOrderIssuedEvent(
        String eventId,
        String eventType,
        String version,
        String source,
        Instant timestamp,
        UUID traceId,
        Payload payload) {

    public static PurchaseOrderIssuedEvent create(PurchaseOrder purchaseOrder, Instant timestamp) {
        Objects.requireNonNull(purchaseOrder, "purchaseOrder must not be null");
        return new PurchaseOrderIssuedEvent(
                UUID.randomUUID().toString(),
                "PO_ISSUED",
                "1.0",
                "finance-service",
                Objects.requireNonNull(timestamp, "timestamp must not be null"),
                UUID.randomUUID(),
                Payload.from(purchaseOrder));
    }

    public PurchaseOrderIssuedEvent {
        eventId = requireText(eventId, "eventId");
        eventType = requireText(eventType, "eventType");
        version = requireText(version, "version");
        source = requireText(source, "source");
        timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        traceId = Objects.requireNonNull(traceId, "traceId must not be null");
        payload = Objects.requireNonNull(payload, "payload must not be null");
    }

    public record Payload(
            UUID poId,
            String poNumber,
            UUID prId,
            String prNumber,
            UUID vendorId,
            String vendorName,
            String vendorEmail,
            String vendorTaxCode,
            UUID purchasingOfficerId,
            String recipientId,
            String referenceType,
            UUID referenceId,
            String referenceNumber,
            String actionUrl,
            BigDecimal totalAmount,
            String currency,
            String deliveryAddress,
            LocalDate deliveryDeadline,
            String paymentTerms,
            Instant issuedAt,
            Instant sentToVendorAt,
            List<LineItem> lineItems) {

        public static Payload from(PurchaseOrder purchaseOrder) {
            return new Payload(
                    purchaseOrder.id(),
                    purchaseOrder.poNumber(),
                    purchaseOrder.prId(),
                    purchaseOrder.prNumber(),
                    purchaseOrder.vendorId(),
                    purchaseOrder.vendorName(),
                    purchaseOrder.vendorEmail(),
                    purchaseOrder.vendorTaxCode(),
                    purchaseOrder.purchasingOfficerId(),
                    purchaseOrder.purchasingOfficerId().toString(),
                    "PURCHASE_ORDER",
                    purchaseOrder.id(),
                    purchaseOrder.poNumber(),
                    "/finance/purchase-orders/" + purchaseOrder.id(),
                    purchaseOrder.totalAmount().amount(),
                    purchaseOrder.totalAmount().currency(),
                    purchaseOrder.deliveryAddress(),
                    purchaseOrder.deliveryDeadline(),
                    purchaseOrder.paymentTerms(),
                    purchaseOrder.issuedAt(),
                    purchaseOrder.sentToVendorAt(),
                    purchaseOrder.lineItems().stream()
                            .map(LineItem::from)
                            .toList());
        }
    }

    public record LineItem(
            UUID poLineItemId,
            UUID prLineItemId,
            String itemName,
            String categoryCode,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String currency) {

        public static LineItem from(PurchaseOrderLineItem lineItem) {
            return new LineItem(
                    lineItem.id(),
                    lineItem.prLineItemId(),
                    lineItem.itemName(),
                    lineItem.categoryCode(),
                    lineItem.quantity(),
                    lineItem.unit(),
                    lineItem.unitPrice().amount(),
                    lineItem.totalPrice().amount(),
                    lineItem.totalPrice().currency());
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
