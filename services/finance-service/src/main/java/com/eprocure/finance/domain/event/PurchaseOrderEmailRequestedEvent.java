package com.eprocure.finance.domain.event;

import com.eprocure.finance.domain.model.PurchaseOrder;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PurchaseOrderEmailRequestedEvent(
        String eventId,
        String eventType,
        String source,
        Instant timestamp,
        Payload payload) {
    private static final String EMAIL_SEND_EVENT_TYPE = "notification.email.send";
    private static final String TEMPLATE_EVENT_TYPE = "PO_ISSUED";
    private static final String SOURCE = "finance-service";

    public static PurchaseOrderEmailRequestedEvent create(PurchaseOrder purchaseOrder, Instant timestamp) {
        Objects.requireNonNull(purchaseOrder, "purchaseOrder must not be null");
        return new PurchaseOrderEmailRequestedEvent(
                UUID.randomUUID().toString(),
                EMAIL_SEND_EVENT_TYPE,
                SOURCE,
                Objects.requireNonNull(timestamp, "timestamp must not be null"),
                Payload.from(purchaseOrder));
    }

    public PurchaseOrderEmailRequestedEvent {
        eventId = requireText(eventId, "eventId");
        eventType = requireText(eventType, "eventType");
        source = requireText(source, "source");
        timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        payload = Objects.requireNonNull(payload, "payload must not be null");
    }

    public record Payload(
            String templateEventType,
            String recipientId,
            String recipientEmail,
            String language,
            String referenceType,
            String referenceId,
            String referenceNumber,
            String actionUrl,
            String subject,
            String body,
            String poNumber,
            String vendorName,
            String totalAmount,
            String currency,
            String deliveryAddress,
            String deliveryDeadline,
            String paymentTerms) {

        public static Payload from(PurchaseOrder purchaseOrder) {
            return new Payload(
                    TEMPLATE_EVENT_TYPE,
                    purchaseOrder.purchasingOfficerId().toString(),
                    purchaseOrder.vendorEmail(),
                    "vi",
                    "PURCHASE_ORDER",
                    purchaseOrder.id().toString(),
                    purchaseOrder.poNumber(),
                    "/finance/purchase-orders/" + purchaseOrder.id(),
                    "Purchase Order " + purchaseOrder.poNumber(),
                    PurchaseOrderEmailRequestedEvent.body(purchaseOrder),
                    purchaseOrder.poNumber(),
                    purchaseOrder.vendorName(),
                    purchaseOrder.totalAmount().amount().toPlainString(),
                    purchaseOrder.totalAmount().currency(),
                    purchaseOrder.deliveryAddress(),
                    purchaseOrder.deliveryDeadline() == null ? null : purchaseOrder.deliveryDeadline().toString(),
                    purchaseOrder.paymentTerms());
        }
    }

    private static String body(PurchaseOrder purchaseOrder) {
        StringBuilder builder = new StringBuilder();
        builder.append("Kinh gui ").append(purchaseOrder.vendorName()).append(",\n\n");
        builder.append("Don dat hang ").append(purchaseOrder.poNumber()).append(" da duoc phat hanh.\n");
        builder.append("Tong gia tri: ")
                .append(purchaseOrder.totalAmount().amount().toPlainString())
                .append(' ')
                .append(purchaseOrder.totalAmount().currency())
                .append('\n');
        if (purchaseOrder.deliveryAddress() != null) {
            builder.append("Dia chi giao hang: ").append(purchaseOrder.deliveryAddress()).append('\n');
        }
        if (purchaseOrder.deliveryDeadline() != null) {
            builder.append("Han giao hang: ").append(purchaseOrder.deliveryDeadline()).append('\n');
        }
        if (purchaseOrder.paymentTerms() != null) {
            builder.append("Dieu khoan thanh toan: ").append(purchaseOrder.paymentTerms()).append('\n');
        }
        if (purchaseOrder.vendorNote() != null) {
            builder.append("\nGhi chu: ").append(purchaseOrder.vendorNote()).append('\n');
        }
        builder.append("\nTran trong,\neProcure");
        return builder.toString();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
