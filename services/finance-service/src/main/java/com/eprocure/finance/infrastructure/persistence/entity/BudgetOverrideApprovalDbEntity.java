package com.eprocure.finance.infrastructure.persistence.entity;

import com.eprocure.finance.domain.model.BudgetOverrideApproval;
import com.eprocure.finance.domain.model.BudgetOverrideStatus;
import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class BudgetOverrideApprovalDbEntity {
    private UUID id;
    private UUID budgetId;
    private UUID purchaseRequestId;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private UUID approvedBy;
    private Instant approvedAt;
    private UUID idempotencyKey;
    private BudgetOverrideStatus status;

    public static BudgetOverrideApprovalDbEntity from(BudgetOverrideApproval approval) {
        BudgetOverrideApprovalDbEntity entity = new BudgetOverrideApprovalDbEntity();
        entity.id = approval.id();
        entity.budgetId = approval.budgetId();
        entity.purchaseRequestId = approval.purchaseRequestId();
        entity.amount = approval.money().amount();
        entity.currency = approval.money().currency();
        entity.reason = approval.reason();
        entity.approvedBy = approval.approvedBy();
        entity.approvedAt = approval.approvedAt();
        entity.idempotencyKey = approval.idempotencyKey();
        entity.status = approval.status();
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(UUID budgetId) {
        this.budgetId = budgetId;
    }

    public UUID getPurchaseRequestId() {
        return purchaseRequestId;
    }

    public void setPurchaseRequestId(UUID purchaseRequestId) {
        this.purchaseRequestId = purchaseRequestId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Money getMoney() {
        return new Money(amount, currency);
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public BudgetOverrideStatus getStatus() {
        return status;
    }

    public void setStatus(BudgetOverrideStatus status) {
        this.status = status;
    }
}
