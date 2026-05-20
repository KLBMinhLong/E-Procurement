package com.eprocure.pr.presentation.mapper;

import com.eprocure.pr.application.port.in.CancelPurchaseRequestCommand;
import com.eprocure.pr.application.port.in.CreatePurchaseRequestCommand;
import com.eprocure.pr.application.port.in.GetPurchaseRequestQuery;
import com.eprocure.pr.application.port.in.SubmitPurchaseRequestCommand;
import com.eprocure.pr.application.port.in.UpdatePurchaseRequestCommand;
import com.eprocure.pr.application.service.CreatedPurchaseRequestView;
import com.eprocure.pr.application.service.PurchaseRequestDetailView;
import com.eprocure.pr.application.service.PurchaseRequestSummaryView;
import com.eprocure.pr.application.service.SubmittedPurchaseRequestView;
import com.eprocure.pr.application.service.UpdatedPurchaseRequestView;
import com.eprocure.pr.common.security.UserPrincipal;
import com.eprocure.pr.presentation.request.CancelPrRequest;
import com.eprocure.pr.presentation.request.CreatePrRequest;
import com.eprocure.pr.presentation.request.ListPrRequest;
import com.eprocure.pr.presentation.request.PrLineItemRequest;
import com.eprocure.pr.presentation.request.UpdatePrRequest;
import com.eprocure.pr.presentation.response.CreatedPurchaseRequestResponse;
import com.eprocure.pr.presentation.response.PurchaseRequestDetailResponse;
import com.eprocure.pr.presentation.response.PurchaseRequestSummaryResponse;
import com.eprocure.pr.presentation.response.SubmittedPurchaseRequestResponse;
import com.eprocure.pr.presentation.response.UpdatedPurchaseRequestResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PurchaseRequestPresentationMapper {

    // ── Create ───────────────────────────────────────────────────────────────

    public CreatePurchaseRequestCommand toCommand(CreatePrRequest request, UserPrincipal principal) {
        return new CreatePurchaseRequestCommand(
                principal.getId(),
                principal.getDepartmentId(),
                request.title(),
                request.justification(),
                request.priority(),
                request.urgencyReason(),
                request.needByDate(),
                null,
                false,
                toLineItemCommands(request.lineItems()),
                request.attachmentIds());
    }

    public CreatedPurchaseRequestResponse toResponse(CreatedPurchaseRequestView view) {
        return new CreatedPurchaseRequestResponse(view.id(), view.prNumber(), view.status());
    }

    // ── Submit ───────────────────────────────────────────────────────────────

    public SubmitPurchaseRequestCommand toCommand(UUID purchaseRequestId, UserPrincipal principal) {
        return new SubmitPurchaseRequestCommand(purchaseRequestId, principal.getId());
    }

    public SubmittedPurchaseRequestResponse toResponse(SubmittedPurchaseRequestView view) {
        return new SubmittedPurchaseRequestResponse(view.id(), view.prNumber(), view.status());
    }

    // ── Update ───────────────────────────────────────────────────────────────

    public UpdatePurchaseRequestCommand toUpdateCommand(UUID purchaseRequestId, UpdatePrRequest request, UserPrincipal principal) {
        return new UpdatePurchaseRequestCommand(
                purchaseRequestId,
                principal.getId(),
                request.title(),
                request.justification(),
                request.priority(),
                request.urgencyReason(),
                request.needByDate(),
                toUpdateLineItemCommands(request.lineItems()));
    }

    public UpdatedPurchaseRequestResponse toResponse(UpdatedPurchaseRequestView view) {
        return UpdatedPurchaseRequestResponse.from(view);
    }

    // ── Cancel ───────────────────────────────────────────────────────────────

    public CancelPurchaseRequestCommand toCancelCommand(UUID purchaseRequestId, CancelPrRequest request, UserPrincipal principal) {
        return new CancelPurchaseRequestCommand(purchaseRequestId, principal.getId(), request.reason());
    }

    // ── Get Detail ───────────────────────────────────────────────────────────

    public PurchaseRequestDetailResponse toDetailResponse(PurchaseRequestDetailView view) {
        return PurchaseRequestDetailResponse.from(view);
    }

    // ── Get List ─────────────────────────────────────────────────────────────

    public GetPurchaseRequestQuery toQuery(ListPrRequest request, UserPrincipal principal, String viewScope) {
        return new GetPurchaseRequestQuery(
                principal.getId(),
                viewScope,
                request.status(),
                request.priority(),
                request.departmentId(),
                request.requesterId(),
                request.fromDate(),
                request.toDate(),
                request.minAmount(),
                request.maxAmount(),
                request.q(),
                request.page() != null ? request.page() : 1,
                request.size() != null ? request.size() : 20,
                request.sort() != null ? request.sort() : "createdAt,desc");
    }

    public PurchaseRequestSummaryResponse toSummaryResponse(PurchaseRequestSummaryView view) {
        return PurchaseRequestSummaryResponse.from(view);
    }

    // ── Line item helpers ─────────────────────────────────────────────────────

    private List<CreatePurchaseRequestCommand.LineItemCommand> toLineItemCommands(List<PrLineItemRequest> requests) {
        return requests.stream().map(this::toLineItemCommand).toList();
    }

    private CreatePurchaseRequestCommand.LineItemCommand toLineItemCommand(PrLineItemRequest request) {
        return new CreatePurchaseRequestCommand.LineItemCommand(
                request.itemCode(),
                request.itemName(),
                request.description(),
                request.categoryCode(),
                request.quantity().amount(),
                request.quantity().unit(),
                request.unitPrice().amount(),
                request.unitPrice().currency(),
                request.preferredVendorId(),
                request.specifications(),
                request.glAccountCode(),
                request.isFromCatalog());
    }

    private List<UpdatePurchaseRequestCommand.LineItemCommand> toUpdateLineItemCommands(List<PrLineItemRequest> requests) {
        if (requests == null) return List.of();
        return requests.stream().map(this::toUpdateLineItemCommand).toList();
    }

    private UpdatePurchaseRequestCommand.LineItemCommand toUpdateLineItemCommand(PrLineItemRequest request) {
        return new UpdatePurchaseRequestCommand.LineItemCommand(
                request.itemCode(),
                request.itemName(),
                request.description(),
                request.categoryCode(),
                request.quantity().amount(),
                request.quantity().unit(),
                request.unitPrice().amount(),
                request.unitPrice().currency(),
                request.preferredVendorId(),
                request.specifications(),
                request.glAccountCode(),
                request.isFromCatalog());
    }
}
