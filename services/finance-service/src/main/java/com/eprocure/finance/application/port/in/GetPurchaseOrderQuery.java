package com.eprocure.finance.application.port.in;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record GetPurchaseOrderQuery(
        UUID actorId,
        Set<String> permissions,
        UUID purchaseOrderId) {

    public GetPurchaseOrderQuery {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        purchaseOrderId = Objects.requireNonNull(purchaseOrderId, "purchaseOrderId must not be null");
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
