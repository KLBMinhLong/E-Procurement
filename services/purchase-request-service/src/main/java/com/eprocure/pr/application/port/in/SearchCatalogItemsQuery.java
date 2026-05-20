package com.eprocure.pr.application.port.in;

import java.util.UUID;

public record SearchCatalogItemsQuery(
        UUID actorId,
        String q,
        String categoryCode,
        int page,
        int size
) {
    public int offset() {
        return (page - 1) * size;
    }
}
