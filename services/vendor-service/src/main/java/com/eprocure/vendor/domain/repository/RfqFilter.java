package com.eprocure.vendor.domain.repository;

import com.eprocure.vendor.domain.model.RfqStatus;
import java.util.UUID;

public record RfqFilter(
        RfqStatus status,
        UUID prId,
        int page,
        int size,
        int offset,
        String sortField,
        String sortDirection) {
}
