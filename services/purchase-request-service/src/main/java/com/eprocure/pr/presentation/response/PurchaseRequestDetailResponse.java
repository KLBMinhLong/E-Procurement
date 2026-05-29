package com.eprocure.pr.presentation.response;

import com.eprocure.pr.application.service.PurchaseRequestDetailView;
import com.eprocure.pr.domain.model.PrLineItem;
import com.eprocure.pr.domain.model.PrPriority;
import com.eprocure.pr.domain.model.PrStatus;
import com.eprocure.pr.domain.model.vo.BudgetCheckResult;
import com.eprocure.pr.domain.model.vo.InventoryCheckResult;
import com.eprocure.pr.domain.model.vo.Money;
import com.eprocure.pr.domain.model.vo.Quantity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseRequestDetailResponse(
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
        List<LineItemResponse> lineItems,
        BudgetCheckResult budgetCheck,
        InventoryCheckResult inventoryCheck,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy
) {
    public record LineItemResponse(
            UUID id,
            int lineNumber,
            String itemCode,
            String itemName,
            String description,
            String categoryCode,
            Quantity quantity,
            Money unitPrice,
            UUID preferredVendorId,
            String specifications,
            String glAccountCode,
            boolean isFromCatalog,
            Money totalPrice
    ) {}

    public static PurchaseRequestDetailResponse from(PurchaseRequestDetailView view) {
        List<LineItemResponse> lineItems = view.lineItems().stream()
                .map(li -> new LineItemResponse(
                        li.getId(),
                        li.getLineNumber(),
                        li.getItemCode().orElse(null),
                        li.getItemName(),
                        li.getDescription().orElse(null),
                        li.getCategoryCode(),
                        li.getQuantity(),
                        li.getUnitPrice(),
                        li.getPreferredVendorId().orElse(null),
                        li.getSpecifications().orElse(null),
                        li.getGlAccountCode(),
                        li.isFromCatalog(),
                        li.getTotalPrice()))
                .toList();
        return new PurchaseRequestDetailResponse(
                view.id(),
                view.prNumber(),
                view.requesterId(),
                view.departmentId(),
                view.title(),
                view.justification(),
                view.priority(),
                view.urgencyReason(),
                view.status(),
                view.totalAmount(),
                view.fiscalYear(),
                view.needByDate(),
                view.blanketRelease(),
                lineItems,
                view.budgetCheck(),
                view.inventoryCheck(),
                view.submittedAt(),
                view.createdAt(),
                view.updatedAt(),
                view.createdBy());
    }
}
