package com.eprocure.vendor.application.port.in;

import java.util.UUID;

public record ApproveVendorCommand(
        UUID actorId,
        UUID vendorId,
        String notes) {
}
