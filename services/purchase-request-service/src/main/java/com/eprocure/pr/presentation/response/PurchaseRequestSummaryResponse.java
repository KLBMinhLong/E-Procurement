package com.eprocure.pr.presentation.response;

import com.eprocure.pr.application.service.PurchaseRequestSummaryView;
import com.eprocure.pr.domain.model.PrPriority;
import com.eprocure.pr.domain.model.PrStatus;
import com.eprocure.pr.domain.model.vo.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseRequestSummaryResponse(
        UUID id,
        String prNumber,
        String title,
        PrPriority priority,
        PrStatus status,
        Money totalAmount,
        UUID requesterId,
        UUID departmentId,
        LocalDate needByDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static PurchaseRequestSummaryResponse from(PurchaseRequestSummaryView view) {
        return new PurchaseRequestSummaryResponse(
                view.id(),
                view.prNumber(),
                view.title(),
                view.priority(),
                view.status(),
                view.totalAmount(),
                view.requesterId(),
                view.departmentId(),
                view.needByDate(),
                view.createdAt(),
                view.updatedAt());
    }
}
