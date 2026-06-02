package com.eprocure.vendor.application.service;

import com.eprocure.vendor.domain.model.Rfq;
import com.eprocure.vendor.domain.model.RfqStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RfqDetailView(
        UUID id,
        String rfqNumber,
        UUID prId,
        String prNumber,
        String title,
        RfqStatus status,
        Instant submissionDeadline,
        List<RfqLineItemView> lineItems,
        List<RfqInvitationView> invitations,
        List<VendorQuoteView> quotes,
        UUID awardedVendorId,
        UUID awardedQuoteId,
        String awardReason,
        Instant createdAt) {

    public static RfqDetailView from(Rfq rfq) {
        return new RfqDetailView(
                rfq.id(),
                rfq.rfqNumber(),
                rfq.prId(),
                rfq.prNumber(),
                rfq.title(),
                rfq.status(),
                rfq.submissionDeadline(),
                rfq.lineItems().stream().map(RfqLineItemView::from).toList(),
                rfq.invitations().stream().map(RfqInvitationView::from).toList(),
                List.of(),
                rfq.awardedVendorId(),
                rfq.awardedQuoteId(),
                rfq.awardReason(),
                rfq.createdAt());
    }
}
