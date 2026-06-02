package com.eprocure.vendor.domain.repository;

import com.eprocure.vendor.domain.model.VendorStatus;

public record VendorFilter(
        VendorStatus status,
        String category,
        Boolean onAvlOnly,
        String query,
        int page,
        int size,
        int offset,
        String sortField,
        String sortDirection) {
}
