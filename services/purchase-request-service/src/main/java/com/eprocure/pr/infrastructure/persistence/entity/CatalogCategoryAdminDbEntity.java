package com.eprocure.pr.infrastructure.persistence.entity;

import java.math.BigDecimal;

public class CatalogCategoryAdminDbEntity {
    private String code;
    private String name;
    private String parentCode;
    private boolean requiresSpecialApproval;
    private String specialApproverRole;
    private BigDecimal requiresRfqAbove;
    private String currency;
    private boolean isCapex;
    private long itemCount;
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

    public long getItemCount() { return itemCount; }
    public void setItemCount(long itemCount) { this.itemCount = itemCount; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
}
