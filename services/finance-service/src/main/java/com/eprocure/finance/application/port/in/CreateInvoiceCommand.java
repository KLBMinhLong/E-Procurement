package com.eprocure.finance.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreateInvoiceCommand(
        UUID actorId,
        String invoiceNumber,
        UUID vendorId,
        UUID poId,
        LocalDate invoiceDate,
        LocalDate dueDate,
        List<LineItem> lineItems) {

    public CreateInvoiceCommand {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        invoiceNumber = requireText(invoiceNumber, "invoiceNumber");
        vendorId = Objects.requireNonNull(vendorId, "vendorId must not be null");
        poId = Objects.requireNonNull(poId, "poId must not be null");
        invoiceDate = Objects.requireNonNull(invoiceDate, "invoiceDate must not be null");
        dueDate = Objects.requireNonNull(dueDate, "dueDate must not be null");
        lineItems = List.copyOf(Objects.requireNonNull(lineItems, "lineItems must not be null"));
        if (lineItems.isEmpty()) {
            throw new IllegalArgumentException("lineItems must not be empty");
        }
    }

    public record LineItem(
            UUID poLineItemId,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal taxRate) {

        public LineItem {
            poLineItemId = Objects.requireNonNull(poLineItemId, "poLineItemId must not be null");
            description = requireText(description, "description");
            quantity = requirePositive(quantity, "quantity");
            unitPrice = requireNonNegative(unitPrice, "unitPrice");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static BigDecimal requirePositive(BigDecimal value, String fieldName) {
        BigDecimal checked = Objects.requireNonNull(value, fieldName + " must not be null");
        if (checked.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return checked;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        BigDecimal checked = Objects.requireNonNull(value, fieldName + " must not be null");
        if (checked.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return checked;
    }
}
