package com.eprocure.finance.application.service;

public record InvoiceMutationResult(
        boolean replayed,
        InvoiceView view) {

    public static InvoiceMutationResult fresh(InvoiceView view) {
        return new InvoiceMutationResult(false, view);
    }

    public static InvoiceMutationResult replayed(InvoiceView view) {
        return new InvoiceMutationResult(true, view);
    }
}
