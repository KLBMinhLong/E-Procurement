package com.eprocure.vendor.infrastructure.persistence.entity;

import com.eprocure.vendor.domain.model.Rfq;
import com.eprocure.vendor.domain.model.RfqStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RfqDbEntity {
    private UUID id;
    private String rfqNumber;
    private UUID prId;
    private String prNumber;
    private String title;
    private RfqStatus status;
    private Instant submissionDeadline;
    private List<RfqLineItemDbEntity> lineItems = List.of();
    private List<RfqInvitationDbEntity> invitations = List.of();
    private UUID awardedVendorId;
    private UUID awardedQuoteId;
    private String awardReason;
    private String requirements;
    private Instant closedAt;
    private UUID idempotencyKey;
    private Instant createdAt;
    private UUID createdBy;
    private UUID updatedBy;

    public static RfqDbEntity from(Rfq rfq) {
        RfqDbEntity entity = new RfqDbEntity();
        entity.id = rfq.id();
        entity.rfqNumber = rfq.rfqNumber();
        entity.prId = rfq.prId();
        entity.prNumber = rfq.prNumber();
        entity.title = rfq.title();
        entity.status = rfq.status();
        entity.submissionDeadline = rfq.submissionDeadline();
        entity.awardedVendorId = rfq.awardedVendorId();
        entity.awardedQuoteId = rfq.awardedQuoteId();
        entity.awardReason = rfq.awardReason();
        entity.requirements = rfq.requirements();
        entity.closedAt = rfq.closedAt();
        entity.idempotencyKey = rfq.idempotencyKey();
        entity.createdAt = rfq.createdAt();
        entity.createdBy = rfq.createdBy();
        entity.updatedBy = rfq.updatedBy();
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRfqNumber() {
        return rfqNumber;
    }

    public void setRfqNumber(String rfqNumber) {
        this.rfqNumber = rfqNumber;
    }

    public UUID getPrId() {
        return prId;
    }

    public void setPrId(UUID prId) {
        this.prId = prId;
    }

    public String getPrNumber() {
        return prNumber;
    }

    public void setPrNumber(String prNumber) {
        this.prNumber = prNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public RfqStatus getStatus() {
        return status;
    }

    public void setStatus(RfqStatus status) {
        this.status = status;
    }

    public Instant getSubmissionDeadline() {
        return submissionDeadline;
    }

    public void setSubmissionDeadline(Instant submissionDeadline) {
        this.submissionDeadline = submissionDeadline;
    }

    public List<RfqLineItemDbEntity> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<RfqLineItemDbEntity> lineItems) {
        this.lineItems = List.copyOf(lineItems == null ? List.of() : lineItems);
    }

    public List<RfqInvitationDbEntity> getInvitations() {
        return invitations;
    }

    public void setInvitations(List<RfqInvitationDbEntity> invitations) {
        this.invitations = List.copyOf(invitations == null ? List.of() : invitations);
    }

    public UUID getAwardedVendorId() {
        return awardedVendorId;
    }

    public void setAwardedVendorId(UUID awardedVendorId) {
        this.awardedVendorId = awardedVendorId;
    }

    public UUID getAwardedQuoteId() {
        return awardedQuoteId;
    }

    public void setAwardedQuoteId(UUID awardedQuoteId) {
        this.awardedQuoteId = awardedQuoteId;
    }

    public String getAwardReason() {
        return awardReason;
    }

    public void setAwardReason(String awardReason) {
        this.awardReason = awardReason;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
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
