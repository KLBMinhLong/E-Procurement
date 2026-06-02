package com.eprocure.vendor.application.service;

import com.eprocure.vendor.domain.model.RfqInvitation;
import java.time.Instant;
import java.util.UUID;

public record RfqInvitationView(
        UUID id,
        UUID vendorId,
        String vendorName,
        Instant invitedAt,
        boolean hasSubmitted,
        Instant submittedAt) {

    public static RfqInvitationView from(RfqInvitation invitation) {
        return new RfqInvitationView(
                invitation.id(),
                invitation.vendorId(),
                invitation.vendorName(),
                invitation.invitedAt(),
                invitation.hasSubmitted(),
                invitation.submittedAt());
    }
}
