package com.eprocure.pr.presentation.mapper;

import com.eprocure.pr.application.port.in.CreatePurchaseRequestCommand;
import com.eprocure.pr.application.service.CreatedPurchaseRequestView;
import com.eprocure.pr.common.security.UserPrincipal;
import com.eprocure.pr.presentation.request.CreatePrRequest;
import com.eprocure.pr.presentation.request.PrLineItemRequest;
import com.eprocure.pr.presentation.response.CreatedPurchaseRequestResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PurchaseRequestPresentationMapper {

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

    private List<CreatePurchaseRequestCommand.LineItemCommand> toLineItemCommands(List<PrLineItemRequest> requests) {
        return requests.stream()
                .map(this::toLineItemCommand)
                .toList();
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
}
