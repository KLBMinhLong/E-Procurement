package com.eprocure.finance.presentation.mapper;

import com.eprocure.finance.application.port.in.CancelPurchaseOrderCommand;
import com.eprocure.finance.application.port.in.GetPurchaseOrderQuery;
import com.eprocure.finance.application.port.in.ListPurchaseOrdersQuery;
import com.eprocure.finance.application.port.in.SendPurchaseOrderCommand;
import com.eprocure.finance.application.port.in.UpdatePurchaseOrderDraftCommand;
import com.eprocure.finance.application.service.PurchaseOrderLineItemView;
import com.eprocure.finance.application.service.PurchaseOrderView;
import com.eprocure.finance.common.security.UserPrincipal;
import com.eprocure.finance.domain.model.PurchaseOrderStatus;
import com.eprocure.finance.domain.model.vo.Money;
import com.eprocure.finance.presentation.response.PoLineItemResponse;
import com.eprocure.finance.presentation.response.PurchaseOrderResponse;
import com.eprocure.finance.presentation.response.PurchasingOfficerSnapshotResponse;
import com.eprocure.finance.presentation.response.QuantityResponse;
import com.eprocure.finance.presentation.response.VendorSnapshotResponse;
import com.eprocure.finance.presentation.request.CancelPurchaseOrderRequest;
import com.eprocure.finance.presentation.request.SendPurchaseOrderRequest;
import com.eprocure.finance.presentation.request.UpdatePurchaseOrderDraftRequest;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PurchaseOrderPresentationMapper {

    public ListPurchaseOrdersQuery toListQuery(
            UserPrincipal principal,
            PurchaseOrderStatus status,
            UUID vendorId,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size,
            String sort) {
        return new ListPurchaseOrdersQuery(
                principal.getId(),
                principal.getPermissions(),
                status,
                vendorId,
                fromDate,
                toDate,
                page,
                size,
                sort);
    }

    public GetPurchaseOrderQuery toGetQuery(UserPrincipal principal, UUID poId) {
        return new GetPurchaseOrderQuery(
                principal.getId(),
                principal.getPermissions(),
                poId);
    }

    public UpdatePurchaseOrderDraftCommand toUpdateCommand(
            UserPrincipal principal,
            UUID poId,
            UpdatePurchaseOrderDraftRequest request) {
        return new UpdatePurchaseOrderDraftCommand(
                principal.getId(),
                poId,
                request.deliveryAddress(),
                request.deliveryDeadline(),
                request.paymentTerms());
    }

    public SendPurchaseOrderCommand toSendCommand(
            UserPrincipal principal,
            UUID poId,
            SendPurchaseOrderRequest request) {
        return new SendPurchaseOrderCommand(
                principal.getId(),
                poId,
                request == null ? null : request.additionalNote());
    }

    public CancelPurchaseOrderCommand toCancelCommand(
            UserPrincipal principal,
            UUID poId,
            CancelPurchaseOrderRequest request) {
        return new CancelPurchaseOrderCommand(
                principal.getId(),
                poId,
                request.reason());
    }

    public PurchaseOrderResponse toResponse(PurchaseOrderView view) {
        return new PurchaseOrderResponse(
                view.id(),
                view.poNumber(),
                view.prId(),
                view.prNumber(),
                new VendorSnapshotResponse(
                        view.vendor().id(),
                        view.vendor().name(),
                        view.vendor().email(),
                        view.vendor().taxCode()),
                new PurchasingOfficerSnapshotResponse(
                        view.purchasingOfficer().id(),
                        view.purchasingOfficer().fullName()),
                view.status(),
                view.lineItems().stream()
                        .map(this::toResponse)
                        .toList(),
                format(view.totalAmount()),
                view.totalAmount().currency(),
                view.deliveryAddress(),
                view.deliveryDeadline(),
                view.paymentTerms(),
                view.issuedAt(),
                view.sentToVendorAt(),
                view.createdAt());
    }

    private PoLineItemResponse toResponse(PurchaseOrderLineItemView view) {
        return new PoLineItemResponse(
                view.id(),
                view.lineNumber(),
                view.prLineItemId(),
                view.itemName(),
                view.categoryCode(),
                new QuantityResponse(view.quantity().toPlainString(), view.unit()),
                format(view.unitPrice()),
                format(view.totalPrice()),
                view.totalPrice().currency());
    }

    private String format(Money money) {
        return money.amount().toPlainString();
    }
}
