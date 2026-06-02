package com.eprocure.vendor.application.port.in;

import com.eprocure.vendor.domain.model.VendorStatus;
import java.util.UUID;

public record ListVendorsQuery(
        UUID actorId,
        VendorStatus status,
        String category,
        Boolean onAvlOnly,
        String query,
        int page,
        int size,
        String sort) {
}
