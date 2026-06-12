package com.eprocure.inventory.application.service;

public record CatalogItemMutationResult(CatalogItemView item, boolean replayed) {
    public static CatalogItemMutationResult fresh(CatalogItemView item) {
        return new CatalogItemMutationResult(item, false);
    }

    public static CatalogItemMutationResult replayed(CatalogItemView item) {
        return new CatalogItemMutationResult(item, true);
    }
}
