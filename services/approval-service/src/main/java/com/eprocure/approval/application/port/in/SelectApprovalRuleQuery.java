package com.eprocure.approval.application.port.in;

import com.eprocure.approval.domain.model.PurchaseRequestPriority;
import com.eprocure.approval.domain.model.vo.Money;
import java.util.Set;
import java.util.UUID;

public record SelectApprovalRuleQuery(
        UUID purchaseRequestId,
        UUID departmentId,
        Money totalAmount,
        Set<String> categories,
        PurchaseRequestPriority priority) {

    public SelectApprovalRuleQuery {
        if (purchaseRequestId == null) {
            throw new IllegalArgumentException("purchaseRequestId must not be null");
        }
        if (totalAmount == null) {
            throw new IllegalArgumentException("totalAmount must not be null");
        }
        if (priority == null) {
            throw new IllegalArgumentException("priority must not be null");
        }
        categories = categories == null ? Set.of() : Set.copyOf(categories);
    }
}
