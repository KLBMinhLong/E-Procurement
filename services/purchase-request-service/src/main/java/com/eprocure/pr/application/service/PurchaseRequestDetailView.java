package com.eprocure.pr.application.service;

import com.eprocure.pr.domain.model.PrLineItem;
import com.eprocure.pr.domain.model.PrPriority;
import com.eprocure.pr.domain.model.PrStatus;
import com.eprocure.pr.domain.model.PurchaseRequest;
import com.eprocure.pr.domain.model.vo.BudgetCheckResult;
import com.eprocure.pr.domain.model.vo.InventoryCheckResult;
import com.eprocure.pr.domain.model.vo.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Read-only detail view of a PurchaseRequest (for GET /{id}).
 */
public record PurchaseRequestDetailView(
        UUID id,
        String prNumber,
        UUID requesterId,
        UUID departmentId,
        String title,
        String justification,
        PrPriority priority,
        String urgencyReason,
        PrStatus status,
        Money totalAmount,
        int fiscalYear,
        LocalDate needByDate,
        boolean blanketRelease,
        List<PrLineItem> lineItems,
        BudgetCheckResult budgetCheck,
        InventoryCheckResult inventoryCheck,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy
) {
    public static PurchaseRequestDetailView from(PurchaseRequest pr) {
        return new PurchaseRequestDetailView(
                pr.getId(),
                pr.getPrNumber(),
                pr.getRequesterId(),
                pr.getDepartmentId(),
                pr.getTitle(),
                pr.getJustification(),
                pr.getPriority(),
                pr.getUrgencyReason().orElse(null),
                pr.getStatus(),
                pr.getTotalAmount(),
                pr.getFiscalYear(),
                pr.getNeedByDate().orElse(null),
                pr.isBlanketRelease(),
                pr.getLineItems(),
                pr.getBudgetCheck().orElse(null),
                pr.getInventoryCheck().orElse(null),
                pr.getSubmittedAt().orElse(null),
                pr.getCreatedAt(),
                pr.getUpdatedAt().orElse(null),
                pr.getCreatedBy()
        );
    }
}
