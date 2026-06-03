package com.eprocure.finance.application.service;

import com.eprocure.finance.domain.model.Invoice;
import com.eprocure.finance.domain.model.InvoiceStatus;
import com.eprocure.finance.domain.model.MatchStatus;
import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceView(
        UUID id,
        String invoiceNumber,
        VendorView vendor,
        PurchaseOrderView po,
        List<InvoiceLineItemView> lineItems,
        Money subtotal,
        Money taxAmount,
        Money totalAmount,
        LocalDate invoiceDate,
        LocalDate dueDate,
        InvoiceStatus status,
        MatchResultView matchResult,
        Instant createdAt) {

    public static InvoiceView from(Invoice invoice) {
        MatchResultView matchResult = invoice.poMatchStatus() == null && invoice.grMatchStatus() == null
                ? null
                : new MatchResultView(
                        invoice.poMatchStatus(),
                        invoice.grMatchStatus(),
                        invoice.qtyVariance(),
                        invoice.priceVariance(),
                        invoice.matchedAt());
        return new InvoiceView(
                invoice.id(),
                invoice.invoiceNumber(),
                new VendorView(invoice.vendorId(), invoice.vendorName()),
                new PurchaseOrderView(invoice.poId(), invoice.poNumber()),
                invoice.lineItems().stream().map(InvoiceLineItemView::from).toList(),
                invoice.subtotal(),
                invoice.taxAmount(),
                invoice.totalAmount(),
                invoice.invoiceDate(),
                invoice.dueDate(),
                invoice.status(),
                matchResult,
                invoice.createdAt());
    }

    public record VendorView(UUID id, String name) {
    }

    public record PurchaseOrderView(UUID id, String poNumber) {
    }

    public record MatchResultView(
            MatchStatus poMatchStatus,
            MatchStatus grMatchStatus,
            BigDecimal qtyVariance,
            Money priceVariance,
            Instant matchedAt) {
    }
}
