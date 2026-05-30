package com.eprocure.finance.infrastructure.persistence.entity;

import com.eprocure.finance.domain.model.BudgetStatus;
import com.eprocure.finance.domain.model.vo.Money;
import java.math.BigDecimal;
import java.util.UUID;

public class BudgetLedgerSummaryDbEntity {
    private UUID id;
    private UUID departmentId;
    private Integer fiscalYear;
    private Integer quarter;
    private String glAccountCode;
    private BigDecimal allocatedAmount;
    private BigDecimal committedAmount;
    private BigDecimal spentAmount;
    private String currency;
    private BudgetStatus status;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(UUID departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(Integer fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public Integer getQuarter() {
        return quarter;
    }

    public void setQuarter(Integer quarter) {
        this.quarter = quarter;
    }

    public String getGlAccountCode() {
        return glAccountCode;
    }

    public void setGlAccountCode(String glAccountCode) {
        this.glAccountCode = glAccountCode;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    public void setCommittedAmount(BigDecimal committedAmount) {
        this.committedAmount = committedAmount;
    }

    public void setSpentAmount(BigDecimal spentAmount) {
        this.spentAmount = spentAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BudgetStatus getStatus() {
        return status;
    }

    public void setStatus(BudgetStatus status) {
        this.status = status;
    }

    public Money getAllocated() {
        return new Money(allocatedAmount, currency);
    }

    public Money getCommitted() {
        return new Money(committedAmount, currency);
    }

    public Money getSpent() {
        return new Money(spentAmount, currency);
    }
}
