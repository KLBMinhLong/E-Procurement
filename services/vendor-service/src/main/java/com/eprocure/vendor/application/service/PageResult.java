package com.eprocure.vendor.application.service;

import java.util.List;

public record PageResult<T>(List<T> items, PageMeta meta) {
    public PageResult {
        items = List.copyOf(items);
    }
}
