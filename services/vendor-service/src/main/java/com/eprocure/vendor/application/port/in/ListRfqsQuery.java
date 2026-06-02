package com.eprocure.vendor.application.port.in;

import com.eprocure.vendor.domain.model.RfqStatus;
import java.util.UUID;

public record ListRfqsQuery(
        UUID actorId,
        RfqStatus status,
        UUID prId,
        int page,
        int size,
        String sort) {
}
