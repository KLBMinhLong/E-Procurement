package com.eprocure.vendor.presentation.mapper;

import com.eprocure.vendor.application.port.in.CloseRfqCommand;
import com.eprocure.vendor.application.port.in.CreateRfqCommand;
import com.eprocure.vendor.application.port.in.ListRfqsQuery;
import com.eprocure.vendor.application.service.RfqDetailView;
import com.eprocure.vendor.application.service.RfqInvitationView;
import com.eprocure.vendor.application.service.RfqLineItemView;
import com.eprocure.vendor.common.security.UserPrincipal;
import com.eprocure.vendor.domain.model.RfqStatus;
import com.eprocure.vendor.presentation.request.CreateRfqRequest;
import com.eprocure.vendor.presentation.response.RfqDetailResponse;
import com.eprocure.vendor.presentation.response.RfqInvitationResponse;
import com.eprocure.vendor.presentation.response.RfqInvitationVendorResponse;
import com.eprocure.vendor.presentation.response.RfqLineItemResponse;
import com.eprocure.vendor.presentation.response.VendorQuoteResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RfqPresentationMapper {

    public ListRfqsQuery toListQuery(
            UserPrincipal principal,
            RfqStatus status,
            UUID prId,
            int page,
            int size,
            String sort) {
        return new ListRfqsQuery(principal.getId(), status, prId, page, size, sort);
    }

    public CreateRfqCommand toCreateCommand(UserPrincipal principal, CreateRfqRequest request) {
        return new CreateRfqCommand(
                principal.getId(),
                request.prId(),
                request.title(),
                request.submissionDeadline(),
                request.invitedVendorIds(),
                request.requirements());
    }

    public CloseRfqCommand toCloseCommand(UserPrincipal principal, UUID rfqId) {
        return new CloseRfqCommand(principal.getId(), rfqId);
    }

    public RfqDetailResponse toResponse(RfqDetailView view) {
        return new RfqDetailResponse(
                view.id(),
                view.rfqNumber(),
                view.prId(),
                view.prNumber(),
                view.title(),
                view.status(),
                view.submissionDeadline(),
                view.lineItems().stream().map(this::toResponse).toList(),
                view.invitations().stream().map(this::toResponse).toList(),
                view.quotes().stream()
                        .map(quote -> new VendorQuoteResponse(
                                quote.id(),
                                quote.rfqId(),
                                quote.vendorId(),
                                quote.vendorName()))
                        .toList(),
                view.awardedVendorId(),
                view.awardedQuoteId(),
                view.awardReason(),
                view.createdAt());
    }

    private RfqLineItemResponse toResponse(RfqLineItemView view) {
        return new RfqLineItemResponse(
                view.id(),
                view.itemName(),
                view.categoryCode(),
                view.quantity().stripTrailingZeros().toPlainString(),
                view.unit(),
                view.specifications());
    }

    private RfqInvitationResponse toResponse(RfqInvitationView view) {
        return new RfqInvitationResponse(
                view.id(),
                new RfqInvitationVendorResponse(view.vendorId(), view.vendorName()),
                view.invitedAt(),
                view.hasSubmitted(),
                view.submittedAt());
    }
}
