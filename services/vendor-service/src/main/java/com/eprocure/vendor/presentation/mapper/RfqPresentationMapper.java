package com.eprocure.vendor.presentation.mapper;

import com.eprocure.vendor.application.port.in.AwardRfqCommand;
import com.eprocure.vendor.application.port.in.CloseRfqCommand;
import com.eprocure.vendor.application.port.in.CreateRfqCommand;
import com.eprocure.vendor.application.port.in.EvaluateQuoteCommand;
import com.eprocure.vendor.application.port.in.ListRfqsQuery;
import com.eprocure.vendor.application.port.in.SubmitVendorQuoteCommand;
import com.eprocure.vendor.application.service.AwardRfqResult;
import com.eprocure.vendor.application.service.RfqDetailView;
import com.eprocure.vendor.application.service.RfqInvitationView;
import com.eprocure.vendor.application.service.RfqLineItemView;
import com.eprocure.vendor.application.service.VendorQuoteLineItemView;
import com.eprocure.vendor.application.service.VendorQuoteView;
import com.eprocure.vendor.common.security.UserPrincipal;
import com.eprocure.vendor.domain.model.RfqStatus;
import com.eprocure.vendor.presentation.request.AwardRfqRequest;
import com.eprocure.vendor.presentation.request.CreateRfqRequest;
import com.eprocure.vendor.presentation.request.EvaluateQuoteRequest;
import com.eprocure.vendor.presentation.request.SubmitVendorQuoteRequest;
import com.eprocure.vendor.presentation.response.AwardRfqResponse;
import com.eprocure.vendor.presentation.response.AwardedVendorResponse;
import com.eprocure.vendor.presentation.response.RfqDetailResponse;
import com.eprocure.vendor.presentation.response.RfqInvitationResponse;
import com.eprocure.vendor.presentation.response.RfqInvitationVendorResponse;
import com.eprocure.vendor.presentation.response.RfqLineItemResponse;
import com.eprocure.vendor.presentation.response.VendorQuoteLineItemResponse;
import com.eprocure.vendor.presentation.response.VendorQuoteResponse;
import java.math.BigDecimal;
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

    public SubmitVendorQuoteCommand toSubmitQuoteCommand(
            UserPrincipal principal,
            UUID rfqId,
            SubmitVendorQuoteRequest request) {
        return new SubmitVendorQuoteCommand(
                principal.getId(),
                rfqId,
                request.vendorId(),
                request.currency(),
                request.validUntil(),
                request.lineItems().stream()
                        .map(item -> new SubmitVendorQuoteCommand.SubmitVendorQuoteLineItemCommand(
                                item.rfqLineItemId(),
                                item.unitPrice(),
                                item.deliveryDays(),
                                item.warranty()))
                        .toList(),
                request.paymentTerms(),
                request.notes());
    }

    public EvaluateQuoteCommand toEvaluateQuoteCommand(
            UserPrincipal principal,
            UUID rfqId,
            UUID quoteId,
            EvaluateQuoteRequest request) {
        return new EvaluateQuoteCommand(
                principal.getId(),
                rfqId,
                quoteId,
                request.evaluationScore(),
                request.evaluationNote());
    }

    public AwardRfqCommand toAwardCommand(UserPrincipal principal, UUID rfqId, AwardRfqRequest request) {
        return new AwardRfqCommand(
                principal.getId(),
                rfqId,
                request.awardedQuoteId(),
                request.awardReason());
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
                view.quotes().stream().map(this::toResponse).toList(),
                view.awardedVendorId(),
                view.awardedQuoteId(),
                view.awardReason(),
                view.createdAt());
    }

    public VendorQuoteResponse toResponse(VendorQuoteView quote) {
        return new VendorQuoteResponse(
                quote.id(),
                quote.rfqId(),
                quote.vendorId(),
                quote.vendorName(),
                quote.lineItems().stream().map(this::toResponse).toList(),
                money(quote.totalAmount()),
                quote.currency(),
                quote.validUntil(),
                quote.paymentTerms(),
                quote.notes(),
                quote.submittedAt(),
                quote.evaluationScore(),
                quote.evaluationNote());
    }

    public AwardRfqResponse toResponse(AwardRfqResult result) {
        return new AwardRfqResponse(
                new AwardedVendorResponse(result.awardedVendor().id(), result.awardedVendor().name()),
                toResponse(result.awardedQuote()));
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

    private VendorQuoteLineItemResponse toResponse(VendorQuoteLineItemView view) {
        return new VendorQuoteLineItemResponse(
                view.rfqLineItemId(),
                view.itemName(),
                money(view.unitPrice()),
                view.currency(),
                money(view.quantity()),
                money(view.totalPrice()),
                view.deliveryDays(),
                view.warranty());
    }

    private String money(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }
}
