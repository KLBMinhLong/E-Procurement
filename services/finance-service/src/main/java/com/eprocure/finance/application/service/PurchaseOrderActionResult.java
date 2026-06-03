package com.eprocure.finance.application.service;

public record PurchaseOrderActionResult(PurchaseOrderView view, boolean replayed) {
    public static PurchaseOrderActionResult fresh(PurchaseOrderView view) {
        return new PurchaseOrderActionResult(view, false);
    }

    public static PurchaseOrderActionResult replayed(PurchaseOrderView view) {
        return new PurchaseOrderActionResult(view, true);
    }
}
