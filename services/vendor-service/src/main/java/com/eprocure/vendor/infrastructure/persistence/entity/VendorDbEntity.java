package com.eprocure.vendor.infrastructure.persistence.entity;

import com.eprocure.vendor.domain.model.Vendor;
import com.eprocure.vendor.domain.model.VendorStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class VendorDbEntity {
    private UUID id;
    private String vendorCode;
    private String name;
    private String taxCode;
    private String email;
    private String phone;
    private String addressStreet;
    private String addressDistrict;
    private String addressCity;
    private String addressCountry;
    private List<String> categories = List.of();
    private List<VendorContactDbEntity> contacts = List.of();
    private VendorScoreDbEntity scorecard;
    private VendorStatus status;
    private boolean onApprovedVendorList;
    private String notes;
    private UUID approvedBy;
    private Instant approvedAt;
    private UUID idempotencyKey;
    private Instant createdAt;
    private UUID createdBy;
    private UUID updatedBy;

    public static VendorDbEntity from(Vendor vendor) {
        VendorDbEntity entity = new VendorDbEntity();
        entity.id = vendor.id();
        entity.vendorCode = vendor.vendorCode();
        entity.name = vendor.name();
        entity.taxCode = vendor.taxCode();
        entity.email = vendor.email();
        entity.phone = vendor.phone();
        entity.addressStreet = vendor.addressStreet();
        entity.addressDistrict = vendor.addressDistrict();
        entity.addressCity = vendor.addressCity();
        entity.addressCountry = vendor.addressCountry();
        entity.categories = vendor.categories();
        entity.status = vendor.status();
        entity.onApprovedVendorList = vendor.onApprovedVendorList();
        entity.notes = vendor.notes();
        entity.approvedBy = vendor.approvedBy();
        entity.approvedAt = vendor.approvedAt();
        entity.idempotencyKey = vendor.idempotencyKey();
        entity.createdAt = vendor.createdAt();
        entity.createdBy = vendor.createdBy();
        entity.updatedBy = vendor.updatedBy();
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public void setVendorCode(String vendorCode) {
        this.vendorCode = vendorCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
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

    public String getAddressStreet() {
        return addressStreet;
    }

    public void setAddressStreet(String addressStreet) {
        this.addressStreet = addressStreet;
    }

    public String getAddressDistrict() {
        return addressDistrict;
    }

    public void setAddressDistrict(String addressDistrict) {
        this.addressDistrict = addressDistrict;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = List.copyOf(categories == null ? List.of() : categories);
    }

    public List<VendorContactDbEntity> getContacts() {
        return contacts;
    }

    public void setContacts(List<VendorContactDbEntity> contacts) {
        this.contacts = List.copyOf(contacts == null ? List.of() : contacts);
    }

    public VendorScoreDbEntity getScorecard() {
        return scorecard;
    }

    public void setScorecard(VendorScoreDbEntity scorecard) {
        this.scorecard = scorecard;
    }

    public VendorStatus getStatus() {
        return status;
    }

    public void setStatus(VendorStatus status) {
        this.status = status;
    }

    public boolean isOnApprovedVendorList() {
        return onApprovedVendorList;
    }

    public void setOnApprovedVendorList(boolean onApprovedVendorList) {
        this.onApprovedVendorList = onApprovedVendorList;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(UUID idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }
}
