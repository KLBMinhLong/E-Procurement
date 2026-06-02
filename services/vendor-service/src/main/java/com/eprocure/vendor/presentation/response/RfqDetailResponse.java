package com.eprocure.vendor.presentation.response;

import com.eprocure.vendor.domain.model.RfqStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RfqDetailResponse(
        UUID id,
        String rfqNumber,
        UUID prId,
        String prNumber,
        String title,
        RfqStatus status,
        Instant submissionDeadline,
        List<RfqLineItemResponse> lineItems,
        List<RfqInvitationResponse> invitations,
        List<VendorQuoteResponse> quotes,
        UUID awardedVendorId,
        UUID awardedQuoteId,
        String awardReason,
        Instant createdAt) {
}
