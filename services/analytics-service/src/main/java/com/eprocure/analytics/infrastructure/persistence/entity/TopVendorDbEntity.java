package com.eprocure.analytics.infrastructure.persistence.entity;

import com.eprocure.analytics.domain.model.TopVendor;
import java.math.BigDecimal;

public class TopVendorDbEntity {
    private String vendorName;
    private BigDecimal totalSpent;
    private Integer orderCount;
    private BigDecimal avgScore;

    public TopVendor toDomain() {
        return new TopVendor(vendorName, totalSpent, orderCount == null ? 0 : orderCount, avgScore);
    }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }
    public Integer getOrderCount() { return orderCount; }
    public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }
    public BigDecimal getAvgScore() { return avgScore; }
    public void setAvgScore(BigDecimal avgScore) { this.avgScore = avgScore; }
}
