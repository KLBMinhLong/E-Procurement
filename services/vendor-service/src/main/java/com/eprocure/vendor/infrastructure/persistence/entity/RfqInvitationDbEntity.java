package com.eprocure.vendor.infrastructure.persistence.entity;

import com.eprocure.vendor.domain.model.RfqInvitation;
import java.time.Instant;
import java.util.UUID;

public class RfqInvitationDbEntity {
    private UUID id;
    private UUID rfqId;
    private UUID vendorId;
    private String vendorName;
    private Instant invitedAt;
    private boolean hasSubmitted;
    private Instant submittedAt;
    private UUID createdBy;

    public static RfqInvitationDbEntity from(UUID rfqId, RfqInvitation invitation, UUID actorId) {
        RfqInvitationDbEntity entity = new RfqInvitationDbEntity();
        entity.id = invitation.id();
        entity.rfqId = rfqId;
        entity.vendorId = invitation.vendorId();
        entity.vendorName = invitation.vendorName();
        entity.invitedAt = invitation.invitedAt();
        entity.hasSubmitted = invitation.hasSubmitted();
        entity.submittedAt = invitation.submittedAt();
        entity.createdBy = actorId;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRfqId() {
        return rfqId;
    }

    public void setRfqId(UUID rfqId) {
        this.rfqId = rfqId;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public void setVendorId(UUID vendorId) {
        this.vendorId = vendorId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public Instant getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(Instant invitedAt) {
        this.invitedAt = invitedAt;
    }

    public boolean isHasSubmitted() {
        return hasSubmitted;
    }

    public void setHasSubmitted(boolean hasSubmitted) {
        this.hasSubmitted = hasSubmitted;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
}
