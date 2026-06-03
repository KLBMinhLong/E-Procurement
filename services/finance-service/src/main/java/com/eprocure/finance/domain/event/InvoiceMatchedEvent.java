package com.eprocure.finance.domain.event;

import com.eprocure.finance.domain.model.Invoice;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record InvoiceMatchedEvent(
        String eventId,
        String eventType,
        String version,
        String source,
        Instant timestamp,
        UUID traceId,
        Payload payload) {

    public static InvoiceMatchedEvent create(Invoice invoice, Instant timestamp) {
        Objects.requireNonNull(invoice, "invoice must not be null");
        return new InvoiceMatchedEvent(
                UUID.randomUUID().toString(),
                "INVOICE_MATCHED",
                "1.0",
                "finance-service",
                Objects.requireNonNull(timestamp, "timestamp must not be null"),
                UUID.randomUUID(),
                Payload.from(invoice));
    }

    public InvoiceMatchedEvent {
        eventId = requireText(eventId, "eventId");
        eventType = requireText(eventType, "eventType");
        version = requireText(version, "version");
        source = requireText(source, "source");
        timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        traceId = Objects.requireNonNull(traceId, "traceId must not be null");
        payload = Objects.requireNonNull(payload, "payload must not be null");
    }

    public record Payload(
            UUID invoiceId,
            String invoiceNumber,
            UUID poId,
            String poNumber,
            UUID vendorId,
            String vendorName,
            String recipientId,
            String referenceType,
            UUID referenceId,
            String referenceNumber,
            String actionUrl,
            BigDecimal totalAmount,
            String currency,
            LocalDate dueDate,
            Instant matchedAt) {

        public static Payload from(Invoice invoice) {
            return new Payload(
                    invoice.id(),
                    invoice.invoiceNumber(),
                    invoice.poId(),
                    invoice.poNumber(),
                    invoice.vendorId(),
                    invoice.vendorName(),
                    invoice.createdBy().toString(),
                    "INVOICE",
                    invoice.id(),
                    invoice.invoiceNumber(),
                    "/finance/invoices/" + invoice.id(),
                    invoice.totalAmount().amount(),
                    invoice.totalAmount().currency(),
                    invoice.dueDate(),
                    invoice.matchedAt());
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
