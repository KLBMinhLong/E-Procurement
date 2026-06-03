package com.eprocure.finance.domain.model;

import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

public record InvoiceLineItem(
        UUID id,
        int lineNumber,
        String description,
        BigDecimal quantity,
        Money unitPrice,
        BigDecimal taxRate,
        Money taxAmount,
        Money totalPrice) {

    public InvoiceLineItem {
        id = Objects.requireNonNull(id, "id must not be null");
        if (lineNumber < 1) {
            throw new IllegalArgumentException("lineNumber must be positive");
        }
        description = requireText(description, "description");
        quantity = Objects.requireNonNull(quantity, "quantity must not be null").setScale(4, RoundingMode.HALF_UP);
        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        unitPrice = Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        taxRate = Objects.requireNonNull(taxRate, "taxRate must not be null").setScale(4, RoundingMode.HALF_UP);
        if (taxRate.signum() < 0 || taxRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("taxRate must be between 0 and 1");
        }
        taxAmount = Objects.requireNonNull(taxAmount, "taxAmount must not be null");
        totalPrice = Objects.requireNonNull(totalPrice, "totalPrice must not be null");
        if (!unitPrice.currency().equals(taxAmount.currency()) || !unitPrice.currency().equals(totalPrice.currency())) {
            throw new IllegalArgumentException("line item currency mismatch");
        }
    }

    public static InvoiceLineItem create(
            int lineNumber,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal taxRate,
            String currency) {
        BigDecimal normalizedQuantity = Objects.requireNonNull(quantity, "quantity must not be null")
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal normalizedUnitPrice = Objects.requireNonNull(unitPrice, "unitPrice must not be null")
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal normalizedTaxRate = taxRate == null
                ? new BigDecimal("0.1000")
                : taxRate.setScale(4, RoundingMode.HALF_UP);
        BigDecimal lineSubtotal = normalizedQuantity.multiply(normalizedUnitPrice)
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal lineTax = lineSubtotal.multiply(normalizedTaxRate)
                .setScale(4, RoundingMode.HALF_UP);
        return new InvoiceLineItem(
                UUID.randomUUID(),
                lineNumber,
                description,
                normalizedQuantity,
                new Money(normalizedUnitPrice, currency),
                normalizedTaxRate,
                new Money(lineTax, currency),
                new Money(lineSubtotal, currency));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
