package com.eprocure.vendor.infrastructure.persistence.entity;

import com.eprocure.vendor.domain.model.VendorContact;
import java.util.UUID;

public class VendorContactDbEntity {
    private UUID id;
    private UUID vendorId;
    private String name;
    private String role;
    private String email;
    private String phone;
    private boolean primary;
    private UUID createdBy;

    public static VendorContactDbEntity from(UUID vendorId, VendorContact contact, UUID actorId) {
        VendorContactDbEntity entity = new VendorContactDbEntity();
        entity.id = contact.id();
        entity.vendorId = vendorId;
        entity.name = contact.name();
        entity.role = contact.role();
        entity.email = contact.email();
        entity.phone = contact.phone();
        entity.primary = contact.primary();
        entity.createdBy = actorId;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public void setVendorId(UUID vendorId) {
        this.vendorId = vendorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
}
