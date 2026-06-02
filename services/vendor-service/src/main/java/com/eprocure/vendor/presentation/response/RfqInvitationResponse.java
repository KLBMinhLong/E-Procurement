package com.eprocure.vendor.presentation.response;

import java.time.Instant;
import java.util.UUID;

public record RfqInvitationResponse(
        UUID id,
        RfqInvitationVendorResponse vendor,
        Instant invitedAt,
        boolean hasSubmitted,
        Instant submittedAt) {
}
