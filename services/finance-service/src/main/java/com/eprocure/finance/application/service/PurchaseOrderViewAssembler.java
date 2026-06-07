package com.eprocure.finance.application.service;

import com.eprocure.finance.domain.model.PurchaseOrder;
import com.eprocure.finance.domain.repository.PoPrConversionCallbackRepository;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class PurchaseOrderViewAssembler {
    private final PoPrConversionCallbackRepository callbackRepository;

    public PurchaseOrderViewAssembler(PoPrConversionCallbackRepository callbackRepository) {
        this.callbackRepository = callbackRepository;
    }

    public PurchaseOrderView toView(PurchaseOrder purchaseOrder) {
        return PurchaseOrderView.from(
                purchaseOrder,
                callbackRepository.findStatusByPoId(purchaseOrder.id()).orElse(null));
    }

    public List<PurchaseOrderView> toViews(List<PurchaseOrder> purchaseOrders) {
        if (purchaseOrders.isEmpty()) {
            return List.of();
        }
        var statuses = callbackRepository.findStatusesByPoIds(
                purchaseOrders.stream().map(PurchaseOrder::id).toList());
        return purchaseOrders.stream()
                .map(purchaseOrder -> PurchaseOrderView.from(purchaseOrder, statuses.get(purchaseOrder.id())))
                .toList();
    }
}
