package com.eprocure.finance.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

public class ReceivedQuantityDbEntity {
    private UUID poLineItemId;
    private BigDecimal receivedQuantity;

    public UUID getPoLineItemId() { return poLineItemId; }
    public void setPoLineItemId(UUID poLineItemId) { this.poLineItemId = poLineItemId; }
    public BigDecimal getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(BigDecimal receivedQuantity) { this.receivedQuantity = receivedQuantity; }
}
