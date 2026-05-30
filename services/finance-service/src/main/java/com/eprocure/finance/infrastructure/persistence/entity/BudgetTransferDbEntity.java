package com.eprocure.finance.infrastructure.persistence.entity;

import com.eprocure.finance.domain.model.BudgetTransfer;
import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class BudgetTransferDbEntity {
    private UUID id;
    private UUID sourceBudgetId;
    private UUID targetBudgetId;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private UUID approvedBy;
    private Instant approvedAt;
    private UUID idempotencyKey;

    public static BudgetTransferDbEntity from(BudgetTransfer transfer) {
        BudgetTransferDbEntity entity = new BudgetTransferDbEntity();
        entity.id = transfer.id();
        entity.sourceBudgetId = transfer.sourceBudgetId();
        entity.targetBudgetId = transfer.targetBudgetId();
        entity.amount = transfer.money().amount();
        entity.currency = transfer.money().currency();
        entity.reason = transfer.reason();
        entity.approvedBy = transfer.approvedBy();
        entity.approvedAt = transfer.approvedAt();
        entity.idempotencyKey = transfer.idempotencyKey();
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSourceBudgetId() {
        return sourceBudgetId;
    }

    public void setSourceBudgetId(UUID sourceBudgetId) {
        this.sourceBudgetId = sourceBudgetId;
    }

    public UUID getTargetBudgetId() {
        return targetBudgetId;
    }

    public void setTargetBudgetId(UUID targetBudgetId) {
        this.targetBudgetId = targetBudgetId;
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
}
