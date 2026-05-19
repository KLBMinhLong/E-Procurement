package com.eprocure.pr.domain.model;

import com.eprocure.pr.domain.model.vo.Money;
import com.eprocure.pr.domain.model.vo.Quantity;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class PrLineItem {
    private UUID id;
    private UUID purchaseRequestId;
    private int lineNumber;
    private String itemCode;
    private String itemName;
    private String description;
    private String categoryCode;
    private Quantity quantity;
    private Money unitPrice;
    private Money totalPrice;
    private UUID preferredVendorId;
    private String specifications;
    private String glAccountCode;
    private boolean fromCatalog;

    private PrLineItem() {
    }

    private PrLineItem(
            UUID id,
            String itemCode,
            String itemName,
            String description,
            String categoryCode,
            Quantity quantity,
            Money unitPrice,
            UUID preferredVendorId,
            String specifications,
            String glAccountCode,
            boolean fromCatalog) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.itemCode = normalizeOptionalText(itemCode);
        if (fromCatalog && this.itemCode == null) {
            throw new IllegalArgumentException("itemCode is required when line item comes from catalog");
        }
        this.itemName = requireText(itemName, "itemName");
        this.description = normalizeOptionalText(description);
        this.categoryCode = requireText(categoryCode, "categoryCode");
        this.quantity = Objects.requireNonNull(quantity, "quantity must not be null");
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        this.totalPrice = this.unitPrice.multiply(this.quantity.amount());
        this.preferredVendorId = preferredVendorId;
        this.specifications = normalizeOptionalText(specifications);
        this.glAccountCode = requireText(glAccountCode, "glAccountCode");
        this.fromCatalog = fromCatalog;
    }

    public static PrLineItem create(
            String itemCode,
            String itemName,
            String description,
            String categoryCode,
            Quantity quantity,
            Money unitPrice,
            UUID preferredVendorId,
            String specifications,
            String glAccountCode,
            boolean fromCatalog) {
        return new PrLineItem(
                UUID.randomUUID(),
                itemCode,
                itemName,
                description,
                categoryCode,
                quantity,
                unitPrice,
                preferredVendorId,
                specifications,
                glAccountCode,
                fromCatalog);
    }

    public void attachTo(UUID purchaseRequestId, int lineNumber) {
        this.purchaseRequestId = Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
        if (lineNumber <= 0) {
            throw new IllegalArgumentException("lineNumber must be positive");
        }
        this.lineNumber = lineNumber;
    }

    public UUID getId() {
        return id;
    }

    public Optional<UUID> getPurchaseRequestId() {
        return Optional.ofNullable(purchaseRequestId);
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public Optional<String> getItemCode() {
        return Optional.ofNullable(itemCode);
    }

    public String getItemName() {
        return itemName;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Money getTotalPrice() {
        return totalPrice;
    }

    public Optional<UUID> getPreferredVendorId() {
        return Optional.ofNullable(preferredVendorId);
    }

    public Optional<String> getSpecifications() {
        return Optional.ofNullable(specifications);
    }

    public String getGlAccountCode() {
        return glAccountCode;
    }

    public boolean isFromCatalog() {
        return fromCatalog;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
