package com.eprocure.pr.application.service;

import com.eprocure.pr.domain.model.PrPriority;
import com.eprocure.pr.domain.model.PrStatus;
import com.eprocure.pr.domain.model.PurchaseRequest;
import com.eprocure.pr.domain.model.vo.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only view returned from UpdatePurchaseRequestUseCase.
 */
public record UpdatedPurchaseRequestView(
        UUID id,
        String prNumber,
        PrStatus status,
        String title,
        String justification,
        PrPriority priority,
        String urgencyReason,
        LocalDate needByDate,
        Money totalAmount,
        int fiscalYear,
        Instant updatedAt
) {
    public static UpdatedPurchaseRequestView from(PurchaseRequest pr) {
        return new UpdatedPurchaseRequestView(
                pr.getId(),
                pr.getPrNumber(),
                pr.getStatus(),
                pr.getTitle(),
                pr.getJustification(),
                pr.getPriority(),
                pr.getUrgencyReason().orElse(null),
                pr.getNeedByDate().orElse(null),
                pr.getTotalAmount(),
                pr.getFiscalYear(),
                pr.getUpdatedAt().orElse(null)
        );
    }
}
