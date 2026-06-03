package com.eprocure.finance.presentation.response;

import com.eprocure.finance.domain.model.PurchaseOrderStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderResponse(
        UUID id,
        String poNumber,
        UUID prId,
        String prNumber,
        VendorSnapshotResponse vendor,
        PurchasingOfficerSnapshotResponse purchasingOfficer,
        PurchaseOrderStatus status,
        List<PoLineItemResponse> lineItems,
        String totalAmount,
        String currency,
        String deliveryAddress,
        LocalDate deliveryDeadline,
        String paymentTerms,
        Instant issuedAt,
        Instant sentToVendorAt,
        Instant createdAt) {
}
