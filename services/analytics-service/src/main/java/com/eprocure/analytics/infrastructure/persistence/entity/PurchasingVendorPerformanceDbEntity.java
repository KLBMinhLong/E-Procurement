package com.eprocure.analytics.infrastructure.persistence.entity;

import com.eprocure.analytics.domain.model.dashboard.VendorPerformance;
import java.math.BigDecimal;

public class PurchasingVendorPerformanceDbEntity {
    private String vendorName;
    private Integer pendingOrders;

    public VendorPerformance toDomain() {
        return new VendorPerformance(
                vendorName,
                BigDecimal.ZERO,
                0,
                pendingOrders == null ? 0 : pendingOrders);
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public Integer getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(Integer pendingOrders) {
        this.pendingOrders = pendingOrders;
    }
}
