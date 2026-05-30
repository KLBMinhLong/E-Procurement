package com.eprocure.pr.domain.model;

import com.eprocure.pr.domain.event.PrCancelledEvent;
import com.eprocure.pr.domain.event.PrApprovalResultEvent;
import com.eprocure.pr.domain.event.PrSubmittedEvent;
import com.eprocure.pr.domain.model.vo.BudgetCheckResult;
import com.eprocure.pr.domain.model.vo.InventoryCheckResult;
import com.eprocure.pr.domain.model.vo.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PurchaseRequest {
    private static final int MIN_JUSTIFICATION_LENGTH = 50;
    private static final int MIN_URGENCY_REASON_LENGTH = 100;

    private UUID id;
    private String prNumber;
    private UUID requesterId;
    private UUID departmentId;
    private String title;
    private String justification;
    private PrPriority priority;
    private String urgencyReason;
    private PrStatus status;
    private List<PrLineItem> lineItems = new ArrayList<>();
    private Money totalAmount;
    private int fiscalYear;
    private LocalDate needByDate;
    private UUID relatedContractId;
    private boolean blanketRelease;
    private BudgetCheckResult budgetCheck;
    private InventoryCheckResult inventoryCheck;
    private Instant submittedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private boolean deleted;
    private Instant deletedAt;
    private UUID deletedBy;
    private final transient List<Object> domainEvents = new ArrayList<>();

    private PurchaseRequest() {
    }

    private PurchaseRequest(
            UUID id,
            String prNumber,
            UUID requesterId,
            UUID departmentId,
            String title,
            String justification,
            PrPriority priority,
            String urgencyReason,
            int fiscalYear,
            LocalDate needByDate,
            UUID relatedContractId,
            boolean blanketRelease,
            List<PrLineItem> lineItems,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.prNumber = requireText(prNumber, "prNumber");
        this.requesterId = Objects.requireNonNull(requesterId, "requesterId must not be null");
        this.departmentId = Objects.requireNonNull(departmentId, "departmentId must not be null");
        this.title = requireText(title, "title");
        this.justification = requireMinLength(justification, "justification", MIN_JUSTIFICATION_LENGTH);
        this.priority = priority == null ? PrPriority.NORMAL : priority;
        this.urgencyReason = normalizeUrgencyReason(this.priority, urgencyReason);
        this.status = PrStatus.DRAFT;
        this.fiscalYear = validateFiscalYear(fiscalYear);
        this.needByDate = needByDate;
        this.relatedContractId = relatedContractId;
        this.blanketRelease = blanketRelease;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.createdBy = requesterId;
        this.deleted = false;
        replaceLineItems(lineItems);
    }

    public static PurchaseRequest create(
            String prNumber,
            UUID requesterId,
            UUID departmentId,
            String title,
            String justification,
            PrPriority priority,
            String urgencyReason,
            int fiscalYear,
            LocalDate needByDate,
            UUID relatedContractId,
            boolean blanketRelease,
            List<PrLineItem> lineItems,
            Instant createdAt) {
        return new PurchaseRequest(
                UUID.randomUUID(),
                prNumber,
                requesterId,
                departmentId,
                title,
                justification,
                priority,
                urgencyReason,
                fiscalYear,
                needByDate,
                relatedContractId,
                blanketRelease,
                lineItems,
                createdAt);
    }

    public boolean canBeEditedBy(UUID actorId) {
        return status.isEditableByRequester() && requesterId.equals(actorId);
    }

    public boolean canBeCancelledBy(UUID actorId) {
        return status.isCancelableByRequester() && requesterId.equals(actorId);
    }

    public void updateDraft(
            UUID actorId,
            String title,
            String justification,
            PrPriority priority,
            String urgencyReason,
            LocalDate needByDate,
            UUID relatedContractId,
            boolean blanketRelease,
            List<PrLineItem> lineItems,
            Instant updatedAt) {
        ensureEditableBy(actorId);
        this.title = requireText(title, "title");
        this.justification = requireMinLength(justification, "justification", MIN_JUSTIFICATION_LENGTH);
        this.priority = priority == null ? PrPriority.NORMAL : priority;
        this.urgencyReason = normalizeUrgencyReason(this.priority, urgencyReason);
        this.needByDate = needByDate;
        this.relatedContractId = relatedContractId;
        this.blanketRelease = blanketRelease;
        this.updatedBy = Objects.requireNonNull(actorId, "actorId must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        replaceLineItems(lineItems);
    }

    public void submit(
            UUID actorId,
            Instant submittedAt,
            BudgetCheckResult budgetCheck,
            InventoryCheckResult inventoryCheck) {
        ensureEditableBy(actorId);
        if (needByDate == null) {
            throw new IllegalStateException("needByDate is required before submit");
        }
        this.budgetCheck = Objects.requireNonNull(budgetCheck, "budgetCheck must not be null");
        this.inventoryCheck = Objects.requireNonNull(inventoryCheck, "inventoryCheck must not be null");
        this.status = PrStatus.SUBMITTED;
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        this.updatedAt = submittedAt;
        this.updatedBy = actorId;
        domainEvents.add(new PrSubmittedEvent(
                id,
                prNumber,
                title,
                requesterId,
                departmentId,
                priority,
                fiscalYear,
                totalAmount,
                submittedCategories()));
    }

    public void markPendingApproval(Instant updatedAt) {
        if (status != PrStatus.SUBMITTED) {
            throw new IllegalStateException("only SUBMITTED purchase request can move to PENDING_APPROVAL");
        }
        this.status = PrStatus.PENDING_APPROVAL;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public void requestChanges(Instant updatedAt) {
        if (status != PrStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("only PENDING_APPROVAL purchase request can request changes");
        }
        this.status = PrStatus.CHANGES_REQUESTED;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        domainEvents.add(new PrApprovalResultEvent(id, prNumber, departmentId, fiscalYear, totalAmount, status));
    }

    public void approve(Instant updatedAt) {
        if (status != PrStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("only PENDING_APPROVAL purchase request can be approved");
        }
        this.status = PrStatus.APPROVED;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        domainEvents.add(new PrApprovalResultEvent(id, prNumber, departmentId, fiscalYear, totalAmount, status));
    }

    public void reject(Instant updatedAt) {
        if (status != PrStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("only PENDING_APPROVAL purchase request can be rejected");
        }
        this.status = PrStatus.REJECTED;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        domainEvents.add(new PrApprovalResultEvent(id, prNumber, departmentId, fiscalYear, totalAmount, status));
    }

    public void convertToPurchaseOrder(Instant updatedAt) {
        if (status != PrStatus.APPROVED) {
            throw new IllegalStateException("only APPROVED purchase request can be converted to PO");
        }
        this.status = PrStatus.CONVERTED_TO_PO;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public void cancel(UUID actorId, String reason, Instant cancelledAt) {
        if (!canBeCancelledBy(actorId)) {
            throw new IllegalStateException("purchase request cannot be cancelled by this actor");
        }
        this.status = PrStatus.CANCELLED;
        this.updatedBy = Objects.requireNonNull(actorId, "actorId must not be null");
        this.updatedAt = Objects.requireNonNull(cancelledAt, "cancelledAt must not be null");
        domainEvents.add(new PrCancelledEvent(id, prNumber, actorId, reason));
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public UUID getId() {
        return id;
    }

    public String getPrNumber() {
        return prNumber;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public String getTitle() {
        return title;
    }

    public String getJustification() {
        return justification;
    }

    public PrPriority getPriority() {
        return priority;
    }

    public Optional<String> getUrgencyReason() {
        return Optional.ofNullable(urgencyReason);
    }

    public PrStatus getStatus() {
        return status;
    }

    public List<PrLineItem> getLineItems() {
        return Collections.unmodifiableList(lineItems);
    }

    private Set<String> submittedCategories() {
        return lineItems.stream()
                .map(PrLineItem::getCategoryCode)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public int getFiscalYear() {
        return fiscalYear;
    }

    public Optional<LocalDate> getNeedByDate() {
        return Optional.ofNullable(needByDate);
    }

    public Optional<UUID> getRelatedContractId() {
        return Optional.ofNullable(relatedContractId);
    }

    public boolean isBlanketRelease() {
        return blanketRelease;
    }

    public Optional<BudgetCheckResult> getBudgetCheck() {
        return Optional.ofNullable(budgetCheck);
    }

    public Optional<InventoryCheckResult> getInventoryCheck() {
        return Optional.ofNullable(inventoryCheck);
    }

    public Optional<Instant> getSubmittedAt() {
        return Optional.ofNullable(submittedAt);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Optional<Instant> getUpdatedAt() {
        return Optional.ofNullable(updatedAt);
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Optional<UUID> getUpdatedBy() {
        return Optional.ofNullable(updatedBy);
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Optional<Instant> getDeletedAt() {
        return Optional.ofNullable(deletedAt);
    }

    public Optional<UUID> getDeletedBy() {
        return Optional.ofNullable(deletedBy);
    }

    private void ensureEditableBy(UUID actorId) {
        if (!canBeEditedBy(actorId)) {
            throw new IllegalStateException("purchase request cannot be edited by this actor");
        }
    }

    private void replaceLineItems(List<PrLineItem> lineItems) {
        if (lineItems == null || lineItems.isEmpty()) {
            throw new IllegalArgumentException("purchase request must have at least one line item");
        }
        Money runningTotal = Money.zero("VND");
        List<PrLineItem> normalizedLineItems = new ArrayList<>();
        int lineNumber = 1;
        for (PrLineItem lineItem : lineItems) {
            PrLineItem item = Objects.requireNonNull(lineItem, "lineItem must not be null");
            item.attachTo(id, lineNumber++);
            runningTotal = runningTotal.add(item.getTotalPrice());
            normalizedLineItems.add(item);
        }
        this.lineItems = normalizedLineItems;
        this.totalAmount = runningTotal;
    }

    private static String normalizeUrgencyReason(PrPriority priority, String urgencyReason) {
        if (!priority.requiresUrgencyReason()) {
            return normalizeOptionalText(urgencyReason);
        }
        return requireMinLength(urgencyReason, "urgencyReason", MIN_URGENCY_REASON_LENGTH);
    }

    private static int validateFiscalYear(int value) {
        if (value < 2000 || value > 2100) {
            throw new IllegalArgumentException("fiscalYear is out of range");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String requireMinLength(String value, String fieldName, int minLength) {
        String normalized = requireText(value, fieldName);
        if (normalized.length() < minLength) {
            throw new IllegalArgumentException(fieldName + " must be at least " + minLength + " characters");
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
