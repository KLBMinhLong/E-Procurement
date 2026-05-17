package com.eprocure.iam.domain.repository;

import java.util.List;

public record Page<T>(List<T> items, long totalElements) {
    public Page {
        items = List.copyOf(items);
    }
}
