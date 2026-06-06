package com.eprocure.finance.application.service;

import com.eprocure.finance.domain.model.PurchaseOrder;
import com.eprocure.finance.domain.model.PoPrConversionCallbackStatus;
import com.eprocure.finance.domain.model.PurchaseOrderStatus;
import com.eprocure.finance.domain.model.vo.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderView(
        UUID id,
        String poNumber,
        UUID prId,
        String prNumber,
        VendorSnapshot vendor,
        PurchasingOfficerSnapshot purchasingOfficer,
        PurchaseOrderStatus status,
        List<PurchaseOrderLineItemView> lineItems,
        Money totalAmount,
        String deliveryAddress,
        LocalDate deliveryDeadline,
        String paymentTerms,
        PoPrConversionCallbackStatus prConversionStatus,
        Instant issuedAt,
        Instant sentToVendorAt,
        Instant createdAt) {

    public static PurchaseOrderView from(PurchaseOrder purchaseOrder) {
        return from(purchaseOrder, null);
    }

    public static PurchaseOrderView from(
            PurchaseOrder purchaseOrder,
            PoPrConversionCallbackStatus prConversionStatus) {
        return new PurchaseOrderView(
                purchaseOrder.id(),
                purchaseOrder.poNumber(),
                purchaseOrder.prId(),
                purchaseOrder.prNumber(),
                new VendorSnapshot(
                        purchaseOrder.vendorId(),
                        purchaseOrder.vendorName(),
                        purchaseOrder.vendorEmail(),
                        purchaseOrder.vendorTaxCode()),
                new PurchasingOfficerSnapshot(
                        purchaseOrder.purchasingOfficerId(),
                        purchaseOrder.purchasingOfficerFullName()),
                purchaseOrder.status(),
                purchaseOrder.lineItems().stream()
                        .map(PurchaseOrderLineItemView::from)
                        .toList(),
                purchaseOrder.totalAmount(),
                purchaseOrder.deliveryAddress(),
                purchaseOrder.deliveryDeadline(),
                purchaseOrder.paymentTerms(),
                prConversionStatus,
                purchaseOrder.issuedAt(),
                purchaseOrder.sentToVendorAt(),
                purchaseOrder.createdAt());
    }

    public record VendorSnapshot(UUID id, String name, String email, String taxCode) {
    }

    public record PurchasingOfficerSnapshot(UUID id, String fullName) {
    }
}
