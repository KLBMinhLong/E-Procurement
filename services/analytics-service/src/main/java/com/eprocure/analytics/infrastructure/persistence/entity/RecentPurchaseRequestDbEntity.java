package com.eprocure.analytics.infrastructure.persistence.entity;

import com.eprocure.analytics.domain.model.dashboard.RecentPurchaseRequest;
import java.math.BigDecimal;
import java.time.Instant;

public class RecentPurchaseRequestDbEntity {
    private String prNumber;
    private String priority;
    private BigDecimal totalAmount;
    private String currency;
    private Instant submittedAt;

    public RecentPurchaseRequest toDomain() {
        return new RecentPurchaseRequest(
                prNumber,
                priority == null ? "" : priority,
                "SUBMITTED",
                totalAmount == null ? BigDecimal.ZERO : totalAmount,
                "",
                null,
                submittedAt);
    }

    public String getPrNumber() {
        return prNumber;
    }

    public void setPrNumber(String prNumber) {
        this.prNumber = prNumber;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }
}
