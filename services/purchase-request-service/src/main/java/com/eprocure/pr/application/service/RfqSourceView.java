package com.eprocure.pr.application.service;

import com.eprocure.pr.domain.model.PrStatus;
import com.eprocure.pr.domain.model.PurchaseRequest;
import java.util.List;
import java.util.UUID;

public record RfqSourceView(
        UUID id,
        String prNumber,
        PrStatus status,
        List<RfqSourceLineItemView> lineItems) {

    public RfqSourceView {
        lineItems = List.copyOf(lineItems);
    }

    public static RfqSourceView from(PurchaseRequest purchaseRequest) {
        return new RfqSourceView(
                purchaseRequest.getId(),
                purchaseRequest.getPrNumber(),
                purchaseRequest.getStatus(),
                purchaseRequest.getLineItems().stream()
                        .map(RfqSourceLineItemView::from)
                        .toList());
    }
}
