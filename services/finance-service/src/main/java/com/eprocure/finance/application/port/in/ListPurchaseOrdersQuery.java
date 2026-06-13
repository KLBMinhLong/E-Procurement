package com.eprocure.finance.application.port.in;

import com.eprocure.finance.domain.model.PurchaseOrderStatus;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record ListPurchaseOrdersQuery(
        UUID actorId,
        Set<String> permissions,
        PurchaseOrderStatus status,
        UUID vendorId,
        UUID prId,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size,
        String sort) {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT = "createdAt,desc";

    public ListPurchaseOrdersQuery {
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        page = page < 1 ? DEFAULT_PAGE : page;
        size = size < 1 || size > MAX_SIZE ? DEFAULT_SIZE : size;
        sort = sort == null || sort.isBlank() ? DEFAULT_SORT : sort.trim();
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
