package com.eprocure.pr.infrastructure.persistence.entity;

import com.eprocure.pr.domain.model.PrPriority;
import com.eprocure.pr.domain.model.PrStatus;
import com.eprocure.pr.domain.model.vo.BudgetCheckResult;
import com.eprocure.pr.domain.model.vo.BudgetCheckStatus;
import com.eprocure.pr.domain.model.vo.InventoryCheckResult;
import com.eprocure.pr.domain.model.vo.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PurchaseRequestDbEntity {
    private UUID id;
    private String prNumber;
    private UUID requesterId;
    private UUID departmentId;
    private String title;
    private String justification;
    private PrPriority priority;
    private String urgencyReason;
    private PrStatus status;
    private List<PrLineItemDbEntity> lineItems = new ArrayList<>();
    private Money totalAmount;
    private int fiscalYear;
    private LocalDate needByDate;
    private UUID relatedContractId;
    private boolean blanketRelease;
    private BudgetCheckResult budgetCheck;
    private BigDecimal budgetAllocatedAmount;
    private BigDecimal budgetCommittedAmount;
    private BigDecimal budgetSpentAmount;
    private BigDecimal budgetAvailableAmount;
    private BudgetCheckStatus budgetCheckStatus;
    private String budgetWarningMessage;
    private InventoryCheckResult inventoryCheck;
    private Instant submittedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private boolean deleted;
    private Instant deletedAt;
    private UUID deletedBy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPrNumber() {
        return prNumber;
    }

    public void setPrNumber(String prNumber) {
        this.prNumber = prNumber;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(UUID requesterId) {
        this.requesterId = requesterId;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(UUID departmentId) {
        this.departmentId = departmentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public PrPriority getPriority() {
        return priority;
    }

    public void setPriority(PrPriority priority) {
        this.priority = priority;
    }

    public String getUrgencyReason() {
        return urgencyReason;
    }

    public void setUrgencyReason(String urgencyReason) {
        this.urgencyReason = urgencyReason;
    }

    public PrStatus getStatus() {
        return status;
    }

    public void setStatus(PrStatus status) {
        this.status = status;
    }

    public List<PrLineItemDbEntity> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<PrLineItemDbEntity> lineItems) {
        this.lineItems = lineItems;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Money totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setTotalAmountAmount(BigDecimal amount) {
        this.totalAmount = new Money(amount, totalAmount == null ? "VND" : totalAmount.currency());
        rebuildBudgetCheck();
    }

    public void setTotalAmountCurrency(String currency) {
        this.totalAmount = new Money(totalAmount == null ? BigDecimal.ZERO : totalAmount.amount(), currency);
        rebuildBudgetCheck();
    }

    public int getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(int fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public LocalDate getNeedByDate() {
        return needByDate;
    }

    public void setNeedByDate(LocalDate needByDate) {
        this.needByDate = needByDate;
    }

    public UUID getRelatedContractId() {
        return relatedContractId;
    }

    public void setRelatedContractId(UUID relatedContractId) {
        this.relatedContractId = relatedContractId;
    }

    public boolean isBlanketRelease() {
        return blanketRelease;
    }

    public void setBlanketRelease(boolean blanketRelease) {
        this.blanketRelease = blanketRelease;
    }

    public BudgetCheckResult getBudgetCheck() {
        return budgetCheck;
    }

    public void setBudgetCheck(BudgetCheckResult budgetCheck) {
        this.budgetCheck = budgetCheck;
    }

    public BigDecimal getBudgetAllocatedAmount() {
        return budgetCheck == null ? budgetAllocatedAmount : budgetCheck.allocated().amount();
    }

    public void setBudgetAllocatedAmount(BigDecimal budgetAllocatedAmount) {
        this.budgetAllocatedAmount = budgetAllocatedAmount;
        rebuildBudgetCheck();
    }

    public BigDecimal getBudgetCommittedAmount() {
        return budgetCheck == null ? budgetCommittedAmount : budgetCheck.committed().amount();
    }

    public void setBudgetCommittedAmount(BigDecimal budgetCommittedAmount) {
        this.budgetCommittedAmount = budgetCommittedAmount;
        rebuildBudgetCheck();
    }

    public BigDecimal getBudgetSpentAmount() {
        return budgetCheck == null ? budgetSpentAmount : budgetCheck.spent().amount();
    }

    public void setBudgetSpentAmount(BigDecimal budgetSpentAmount) {
        this.budgetSpentAmount = budgetSpentAmount;
        rebuildBudgetCheck();
    }

    public BigDecimal getBudgetAvailableAmount() {
        return budgetCheck == null ? budgetAvailableAmount : budgetCheck.available().amount();
    }

    public void setBudgetAvailableAmount(BigDecimal budgetAvailableAmount) {
        this.budgetAvailableAmount = budgetAvailableAmount;
        rebuildBudgetCheck();
    }

    public BudgetCheckStatus getBudgetCheckStatus() {
        return budgetCheck == null ? budgetCheckStatus : budgetCheck.status();
    }

    public void setBudgetCheckStatus(BudgetCheckStatus budgetCheckStatus) {
        this.budgetCheckStatus = budgetCheckStatus;
        rebuildBudgetCheck();
    }

    public String getBudgetWarningMessage() {
        return budgetCheck == null ? budgetWarningMessage : budgetCheck.warningMessage();
    }

    public void setBudgetWarningMessage(String budgetWarningMessage) {
        this.budgetWarningMessage = budgetWarningMessage;
        rebuildBudgetCheck();
    }

    public InventoryCheckResult getInventoryCheck() {
        return inventoryCheck;
    }

    public void setInventoryCheck(InventoryCheckResult inventoryCheck) {
        this.inventoryCheck = inventoryCheck;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
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

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public UUID getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(UUID deletedBy) {
        this.deletedBy = deletedBy;
    }

    private void rebuildBudgetCheck() {
        if (budgetAllocatedAmount == null
                || budgetCommittedAmount == null
                || budgetSpentAmount == null
                || budgetAvailableAmount == null
                || budgetCheckStatus == null) {
            return;
        }
        String currency = totalAmount == null ? "VND" : totalAmount.currency();
        this.budgetCheck = new BudgetCheckResult(
                new Money(budgetAllocatedAmount, currency),
                new Money(budgetCommittedAmount, currency),
                new Money(budgetSpentAmount, currency),
                new Money(budgetAvailableAmount, currency),
                budgetCheckStatus,
                budgetWarningMessage);
    }
}
