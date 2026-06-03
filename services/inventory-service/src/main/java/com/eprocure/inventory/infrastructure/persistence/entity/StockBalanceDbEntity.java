package com.eprocure.inventory.infrastructure.persistence.entity;

import java.math.BigDecimal;

public class StockBalanceDbEntity {
    private String itemCode;
    private BigDecimal quantityOnHand;

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public BigDecimal getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(BigDecimal quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }
}
