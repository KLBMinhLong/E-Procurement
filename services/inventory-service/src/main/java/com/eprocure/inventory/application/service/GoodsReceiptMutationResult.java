package com.eprocure.inventory.application.service;

public record GoodsReceiptMutationResult(GoodsReceiptView view, boolean replayed) {

    public static GoodsReceiptMutationResult fresh(GoodsReceiptView view) {
        return new GoodsReceiptMutationResult(view, false);
    }

    public static GoodsReceiptMutationResult replayed(GoodsReceiptView view) {
        return new GoodsReceiptMutationResult(view, true);
    }
}
