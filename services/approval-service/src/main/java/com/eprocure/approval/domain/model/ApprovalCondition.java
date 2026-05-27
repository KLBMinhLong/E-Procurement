package com.eprocure.approval.domain.model;

import com.eprocure.approval.domain.model.vo.Money;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ApprovalCondition {
    private final Money minValue;
    private final Money maxValue;
    private final Set<String> categories;
    private final Set<UUID> departmentIds;
    private final Set<PurchaseRequestPriority> priorities;

    private ApprovalCondition(
            Money minValue,
            Money maxValue,
            Set<String> categories,
            Set<UUID> departmentIds,
            Set<PurchaseRequestPriority> priorities) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.categories = normalizeCategories(categories);
        this.departmentIds = departmentIds == null ? Set.of() : Set.copyOf(departmentIds);
        this.priorities = priorities == null ? Set.of() : Set.copyOf(priorities);
    }

    public static ApprovalCondition of(
            Money minValue,
            Money maxValue,
            Set<String> categories,
            Set<UUID> departmentIds,
            Set<PurchaseRequestPriority> priorities) {
        return new ApprovalCondition(minValue, maxValue, categories, departmentIds, priorities);
    }

    public boolean matches(
            Money totalAmount,
            Set<String> requestCategories,
            UUID departmentId,
            PurchaseRequestPriority priority) {
        Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        if (minValue != null && totalAmount.compareTo(minValue) < 0) {
            return false;
        }
        if (maxValue != null && totalAmount.compareTo(maxValue) >= 0) {
            return false;
        }
        if (!categories.isEmpty() && !intersects(categories, normalizeCategories(requestCategories))) {
            return false;
        }
        if (!departmentIds.isEmpty() && (departmentId == null || !departmentIds.contains(departmentId))) {
            return false;
        }
        return priorities.isEmpty() || priorities.contains(priority);
    }

    public Money getMinValue() {
        return minValue;
    }

    public Money getMaxValue() {
        return maxValue;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public Set<UUID> getDepartmentIds() {
        return departmentIds;
    }

    public Set<PurchaseRequestPriority> getPriorities() {
        return priorities;
    }

    private static boolean intersects(Set<String> left, Set<String> right) {
        return left.stream().anyMatch(right::contains);
    }

    private static Set<String> normalizeCategories(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
