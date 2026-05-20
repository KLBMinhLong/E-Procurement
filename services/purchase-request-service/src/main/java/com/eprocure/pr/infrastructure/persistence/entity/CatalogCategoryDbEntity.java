package com.eprocure.pr.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;

public class CatalogCategoryDbEntity {
    private String code;
    private String name;
    private String parentCode;
    private boolean requiresSpecialApproval;
    private String specialApproverRole;
    private BigDecimal requiresRfqAbove;
    private String currency;
    private boolean isCapex;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isDeleted;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getParentCode() { return parentCode; }
    public void setParentCode(String parentCode) { this.parentCode = parentCode; }

    public boolean isRequiresSpecialApproval() { return requiresSpecialApproval; }
    public void setRequiresSpecialApproval(boolean requiresSpecialApproval) { this.requiresSpecialApproval = requiresSpecialApproval; }

    public String getSpecialApproverRole() { return specialApproverRole; }
    public void setSpecialApproverRole(String specialApproverRole) { this.specialApproverRole = specialApproverRole; }

    public BigDecimal getRequiresRfqAbove() { return requiresRfqAbove; }
    public void setRequiresRfqAbove(BigDecimal requiresRfqAbove) { this.requiresRfqAbove = requiresRfqAbove; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isCapex() { return isCapex; }
    public void setCapex(boolean capex) { isCapex = capex; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
}
