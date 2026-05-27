package com.eprocure.approval.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ApprovalRuleDbEntity {
    private UUID id;
    private String ruleName;
    private Integer priority;
    private Boolean active;
    private String ruleType;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private Object categories;
    private Object departmentIds;
    private Object priorities;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private List<ApprovalRuleStepDbEntity> steps = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public BigDecimal getMinValue() {
        return minValue;
    }

    public void setMinValue(BigDecimal minValue) {
        this.minValue = minValue;
    }

    public BigDecimal getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
    }

    public Object getCategories() {
        return categories;
    }

    public void setCategories(Object categories) {
        this.categories = categories;
    }

    public Object getDepartmentIds() {
        return departmentIds;
    }

    public void setDepartmentIds(Object departmentIds) {
        this.departmentIds = departmentIds;
    }

    public Object getPriorities() {
        return priorities;
    }

    public void setPriorities(Object priorities) {
        this.priorities = priorities;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public List<ApprovalRuleStepDbEntity> getSteps() {
        return steps;
    }

    public void setSteps(List<ApprovalRuleStepDbEntity> steps) {
        this.steps = steps == null ? new ArrayList<>() : steps;
    }
}
