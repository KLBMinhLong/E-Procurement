package com.eprocure.finance.application.port.out;

import com.eprocure.finance.application.service.PurchaseRequestPoSource;
import java.util.UUID;

public interface PurchaseRequestPoSourcePort {
    PurchaseRequestPoSource fetch(UUID purchaseRequestId);
}
