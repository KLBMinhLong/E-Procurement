package com.eprocure.pr.application.service;

import com.eprocure.pr.domain.model.PrStatus;
import com.eprocure.pr.domain.model.PurchaseRequest;
import com.eprocure.pr.domain.model.vo.Money;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PoSourceView(
        UUID id,
        String prNumber,
        PrStatus status,
        UUID requesterId,
        UUID departmentId,
        int fiscalYear,
        LocalDate needByDate,
        Money totalAmount,
        List<PoSourceLineItemView> lineItems) {

    public PoSourceView {
        lineItems = List.copyOf(lineItems);
    }

    public static PoSourceView from(PurchaseRequest purchaseRequest) {
        return new PoSourceView(
                purchaseRequest.getId(),
                purchaseRequest.getPrNumber(),
                purchaseRequest.getStatus(),
                purchaseRequest.getRequesterId(),
                purchaseRequest.getDepartmentId(),
                purchaseRequest.getFiscalYear(),
                purchaseRequest.getNeedByDate().orElse(null),
                purchaseRequest.getTotalAmount(),
                purchaseRequest.getLineItems().stream()
                        .map(PoSourceLineItemView::from)
                        .toList());
    }
}
