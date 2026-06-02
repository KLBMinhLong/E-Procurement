package com.eprocure.vendor.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateRfqCommand(
        UUID actorId,
        UUID prId,
        String title,
        Instant submissionDeadline,
        List<UUID> invitedVendorIds,
        String requirements) {

    public CreateRfqCommand {
        invitedVendorIds = List.copyOf(invitedVendorIds == null ? List.of() : invitedVendorIds);
    }
}
