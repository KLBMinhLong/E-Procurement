package com.eprocure.pr.presentation.response;

import com.eprocure.pr.application.service.UpdatedPurchaseRequestView;
import com.eprocure.pr.domain.model.PrPriority;
import com.eprocure.pr.domain.model.PrStatus;
import com.eprocure.pr.domain.model.vo.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UpdatedPurchaseRequestResponse(
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
    public static UpdatedPurchaseRequestResponse from(UpdatedPurchaseRequestView view) {
        return new UpdatedPurchaseRequestResponse(
                view.id(),
                view.prNumber(),
                view.status(),
                view.title(),
                view.justification(),
                view.priority(),
                view.urgencyReason(),
                view.needByDate(),
                view.totalAmount(),
                view.fiscalYear(),
                view.updatedAt());
    }
}
