package com.eprocure.pr.domain.model;

import com.eprocure.pr.domain.model.vo.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class CatalogItem {
    private UUID id;
    private String itemCode;
    private String name;
    private String description;
    private String categoryCode;
    private String unit;
    private Money unitPrice;
    private UUID preferredVendorId;
    private BigDecimal reorderPoint;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean deleted;
    private Instant deletedAt;
    private UUID deletedBy;

    // Optional for display logic
    private BigDecimal quantityOnHand;

    private CatalogItem() {
    }

    private CatalogItem(
            UUID id,
            String itemCode,
            String name,
            String description,
            String categoryCode,
            String unit,
            Money unitPrice,
            UUID preferredVendorId,
            BigDecimal reorderPoint,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            boolean deleted,
            Instant deletedAt,
            UUID deletedBy) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.itemCode = Objects.requireNonNull(itemCode, "itemCode must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description;
        this.categoryCode = Objects.requireNonNull(categoryCode, "categoryCode must not be null");
        this.unit = Objects.requireNonNull(unit, "unit must not be null");
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        this.preferredVendorId = preferredVendorId;
        this.reorderPoint = reorderPoint;
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.deletedBy = deletedBy;
    }

    public static CatalogItem reconstitute(
            UUID id,
            String itemCode,
            String name,
            String description,
            String categoryCode,
            String unit,
            Money unitPrice,
            UUID preferredVendorId,
            BigDecimal reorderPoint,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            boolean deleted,
            Instant deletedAt,
            UUID deletedBy) {
        return new CatalogItem(
                id, itemCode, name, description, categoryCode, unit, unitPrice, 
                preferredVendorId, reorderPoint, active, createdAt, updatedAt, 
                deleted, deletedAt, deletedBy);
    }

    public void setQuantityOnHand(BigDecimal quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public UUID getId() {
        return id;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getName() {
        return name;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getUnit() {
        return unit;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Optional<UUID> getPreferredVendorId() {
        return Optional.ofNullable(preferredVendorId);
    }

    public Optional<BigDecimal> getReorderPoint() {
        return Optional.ofNullable(reorderPoint);
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Optional<Instant> getDeletedAt() {
        return Optional.ofNullable(deletedAt);
    }

    public Optional<UUID> getDeletedBy() {
        return Optional.ofNullable(deletedBy);
    }

    public Optional<BigDecimal> getQuantityOnHand() {
        return Optional.ofNullable(quantityOnHand);
    }
}
