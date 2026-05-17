## SK-01 · Domain Model Generation

### Trigger
Agent được yêu cầu tạo entity/aggregate mới hoặc mô hình domain cho một tính năng.

### Inputs Required
- Tên entity và service thuộc về (VD: `PurchaseRequest` → `pr-service`)
- Danh sách fields với kiểu dữ liệu
- Relationships với entity khác
- Business rules bất biến (invariants)

### Rules
```
[R1] Domain model là POJO thuần — ZERO framework annotation (@Entity, @Table, @Column KHÔNG được có)
[R2] Không import bất kỳ Spring, JPA, MyBatis class nào
[R3] Tiền tệ dùng BigDecimal, KHÔNG dùng double/float
[R4] UUID cho tất cả ID — không dùng Long/Integer auto-increment
[R5] Timestamps là Instant (UTC), không phải LocalDateTime
[R6] Aggregate Root phải có domain events list
[R7] Value Objects là immutable (final fields, no setters)
[R8] Enums là UPPER_SNAKE_CASE, có trong package domain/model
```

### Template — Aggregate Root
```java
package com.eprocure.{service}.domain.model;

import com.eprocure.{service}.domain.event.{Entity}Event;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Aggregate Root: {EntityName}
 * <p>Invariants:
 * <ul>
 *   <li>Line items must not be empty on submit</li>
 *   <li>Total amount must be positive</li>
 * </ul>
 */
public class {EntityName} {

    // === Identity ===
    private UUID id;
    private String {entity}Number;     // Business key: PR-YYYY-MM-XXXXX

    // === Core Fields ===
    private UUID requesterId;
    private UUID departmentId;
    private {EntityName}Status status;
    private {EntityName}Priority priority;
    private BigDecimal estimatedTotal;  // ALWAYS BigDecimal for money

    // === Audit ===
    private UUID createdBy;
    private Instant createdAt;
    private UUID updatedBy;
    private Instant updatedAt;
    private boolean isDeleted;
    private Instant deletedAt;
    private UUID deletedBy;

    // === Relations (owned by this aggregate) ===
    private List<{Entity}LineItem> lineItems = new ArrayList<>();

    // === Domain Events (transient, not persisted) ===
    private final transient List<Object> domainEvents = new ArrayList<>();

    // === Constructors ===
    private {EntityName}() {}  // For mapper use only

    public static {EntityName} create(UUID requesterId, UUID departmentId,
                                       {EntityName}Priority priority) {
        {EntityName} entity = new {EntityName}();
        entity.id = UUID.randomUUID();
        entity.requesterId = requesterId;
        entity.departmentId = departmentId;
        entity.priority = priority;
        entity.status = {EntityName}Status.DRAFT;
        entity.createdAt = Instant.now();
        entity.createdBy = requesterId;
        entity.isDeleted = false;
        entity.lineItems = new ArrayList<>();
        return entity;
    }

    // === Business Methods (Domain Logic) ===

    /**
     * Submits this {entity} for approval.
     * @throws IllegalStateException if no line items present
     */
    public void submit() {
        if (lineItems.isEmpty()) {
            throw new IllegalStateException("{EntityName} must have at least one line item");
        }
        if (this.status != {EntityName}Status.DRAFT) {
            throw new IllegalStateException("Only DRAFT {entity} can be submitted");
        }
        this.status = {EntityName}Status.PENDING_APPROVAL;
        this.updatedAt = Instant.now();
        domainEvents.add(new {Entity}SubmittedEvent(this.id, this.requesterId, this.estimatedTotal));
    }

    public void cancel(UUID actorId, String reason) {
        if (this.status == {EntityName}Status.CANCELLED) return;
        this.status = {EntityName}Status.CANCELLED;
        this.updatedBy = actorId;
        this.updatedAt = Instant.now();
        domainEvents.add(new {Entity}CancelledEvent(this.id, actorId, reason));
    }

    public BigDecimal calculateTotal() {
        return lineItems.stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return events;
    }

    // === Getters (NO Setters — use business methods) ===
    public UUID getId() { return id; }
    public {EntityName}Status getStatus() { return status; }
    public BigDecimal getEstimatedTotal() { return estimatedTotal; }
    public List<{Entity}LineItem> getLineItems() { return Collections.unmodifiableList(lineItems); }
    // ... all other getters
}
```

### Template — Value Object
```java
package com.eprocure.{service}.domain.model.vo;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object: {EntityName}Number
 * Immutable. Format: PR-YYYY-MM-XXXXX
 */
public final class {Entity}Number {

    private static final Pattern FORMAT = Pattern.compile("^PR-\\d{4}-\\d{2}-\\d{5}$");
    private final String value;

    private {Entity}Number(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public static {Entity}Number of(String value) {
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid {entity} number format: " + value);
        }
        return new {Entity}Number(value);
    }

    public String getValue() { return value; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof {Entity}Number)) return false;
        return value.equals((({Entity}Number) o).value);
    }

    @Override public int hashCode() { return Objects.hash(value); }
    @Override public String toString() { return value; }
}
```

### Checklist
```
[ ] Không có framework annotation nào
[ ] Tiền tệ là BigDecimal
[ ] ID là UUID
[ ] Timestamps là Instant
[ ] Business methods thay vì setters
[ ] Domain events list (transient)
[ ] Invariant checks trong business methods
[ ] JavaDoc cho class và public methods
```
