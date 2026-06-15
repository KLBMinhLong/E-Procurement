package com.eprocure.iam.application.port.in;

import java.util.UUID;

public record ListActiveSessionsQuery(
        int page,
        int size,
        UUID userId) {

    public ListActiveSessionsQuery {
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 100));
    }

    public int offset() {
        return (page - 1) * size;
    }
}
