package com.eprocure.finance.application.port.out;

import java.util.UUID;

public interface PurchaseRequestConversionCallbackPort {
    void markConverted(UUID purchaseRequestId, UUID purchaseOrderId, String purchaseOrderNumber, UUID idempotencyKey);
}
