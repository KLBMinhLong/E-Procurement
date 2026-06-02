package com.eprocure.vendor.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;

public class VendorScoreDbEntity {
    private int qualityScore;
    private int deliveryScore;
    private int priceScore;
    private int responsivenessScore;
    private int overallScore;
    private Instant lastEvaluatedAt;
    private int totalOrders;
    private BigDecimal onTimeDeliveryRate;

    public int getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(int qualityScore) {
        this.qualityScore = qualityScore;
    }

    public int getDeliveryScore() {
        return deliveryScore;
    }

    public void setDeliveryScore(int deliveryScore) {
        this.deliveryScore = deliveryScore;
    }

    public int getPriceScore() {
        return priceScore;
    }

    public void setPriceScore(int priceScore) {
        this.priceScore = priceScore;
    }

    public int getResponsivenessScore() {
        return responsivenessScore;
    }

    public void setResponsivenessScore(int responsivenessScore) {
        this.responsivenessScore = responsivenessScore;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(int overallScore) {
        this.overallScore = overallScore;
    }

    public Instant getLastEvaluatedAt() {
        return lastEvaluatedAt;
    }

    public void setLastEvaluatedAt(Instant lastEvaluatedAt) {
        this.lastEvaluatedAt = lastEvaluatedAt;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getOnTimeDeliveryRate() {
        return onTimeDeliveryRate;
    }

    public void setOnTimeDeliveryRate(BigDecimal onTimeDeliveryRate) {
        this.onTimeDeliveryRate = onTimeDeliveryRate;
    }
}
