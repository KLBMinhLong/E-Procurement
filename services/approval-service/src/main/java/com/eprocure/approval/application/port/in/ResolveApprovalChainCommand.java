package com.eprocure.approval.application.port.in;

import com.eprocure.approval.domain.model.PurchaseRequestPriority;
import com.eprocure.approval.domain.model.vo.Money;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record ResolveApprovalChainCommand(
        UUID purchaseRequestId,
        UUID requesterId,
        UUID departmentId,
        Money totalAmount,
        Set<String> categories,
        PurchaseRequestPriority priority) {

    public ResolveApprovalChainCommand {
        purchaseRequestId = Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
        requesterId = Objects.requireNonNull(requesterId, "requesterId must not be null");
        departmentId = Objects.requireNonNull(departmentId, "departmentId must not be null");
        totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        priority = Objects.requireNonNull(priority, "priority must not be null");
        categories = categories == null ? Set.of() : Set.copyOf(categories);
    }
}
