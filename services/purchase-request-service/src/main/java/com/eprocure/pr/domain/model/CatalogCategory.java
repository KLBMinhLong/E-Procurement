package com.eprocure.pr.domain.model;

import com.eprocure.pr.domain.model.vo.Money;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CatalogCategory {
    private String code;
    private String name;
    private String parentCode;
    private boolean requiresSpecialApproval;
    private String specialApproverRole;
    private Money requiresRfqAbove;
    private boolean capex;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean deleted;
    private List<CatalogCategory> children = new ArrayList<>();

    private CatalogCategory() {
    }

    private CatalogCategory(
            String code,
            String name,
            String parentCode,
            boolean requiresSpecialApproval,
            String specialApproverRole,
            Money requiresRfqAbove,
            boolean capex,
            Instant createdAt,
            Instant updatedAt,
            boolean deleted) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.parentCode = parentCode;
        this.requiresSpecialApproval = requiresSpecialApproval;
        this.specialApproverRole = specialApproverRole;
        this.requiresRfqAbove = requiresRfqAbove;
        this.capex = capex;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.deleted = deleted;
    }

    public static CatalogCategory create(
            String code,
            String name,
            String parentCode,
            boolean requiresSpecialApproval,
            String specialApproverRole,
            Money requiresRfqAbove,
            boolean capex,
            Instant createdAt) {
        return new CatalogCategory(
                code,
                name,
                parentCode,
                requiresSpecialApproval,
                specialApproverRole,
                requiresRfqAbove,
                capex,
                createdAt,
                createdAt,
                false
        );
    }

    public static CatalogCategory reconstitute(
            String code,
            String name,
            String parentCode,
            boolean requiresSpecialApproval,
            String specialApproverRole,
            Money requiresRfqAbove,
            boolean capex,
            Instant createdAt,
            Instant updatedAt,
            boolean deleted) {
        return new CatalogCategory(
                code,
                name,
                parentCode,
                requiresSpecialApproval,
                specialApproverRole,
                requiresRfqAbove,
                capex,
                createdAt,
                updatedAt,
                deleted
        );
    }

    public void addChild(CatalogCategory child) {
        if (child != null) {
            this.children.add(child);
        }
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Optional<String> getParentCode() {
        return Optional.ofNullable(parentCode);
    }

    public boolean isRequiresSpecialApproval() {
        return requiresSpecialApproval;
    }

    public Optional<String> getSpecialApproverRole() {
        return Optional.ofNullable(specialApproverRole);
    }

    public Optional<Money> getRequiresRfqAbove() {
        return Optional.ofNullable(requiresRfqAbove);
    }

    public boolean isCapex() {
        return capex;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public List<CatalogCategory> getChildren() {
        return Collections.unmodifiableList(children);
    }
}
