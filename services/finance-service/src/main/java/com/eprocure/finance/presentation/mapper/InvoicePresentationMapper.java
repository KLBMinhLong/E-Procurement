package com.eprocure.finance.presentation.mapper;

import com.eprocure.finance.application.port.in.CreateInvoiceCommand;
import com.eprocure.finance.application.port.in.GetInvoiceQuery;
import com.eprocure.finance.application.port.in.ListInvoicesQuery;
import com.eprocure.finance.application.port.in.MatchInvoiceCommand;
import com.eprocure.finance.application.service.InvoiceLineItemView;
import com.eprocure.finance.application.service.InvoiceMatchResult;
import com.eprocure.finance.application.service.InvoiceView;
import com.eprocure.finance.common.security.UserPrincipal;
import com.eprocure.finance.domain.model.InvoiceStatus;
import com.eprocure.finance.domain.model.vo.Money;
import com.eprocure.finance.presentation.request.CreateInvoiceRequest;
import com.eprocure.finance.presentation.response.InvoiceLineItemResponse;
import com.eprocure.finance.presentation.response.InvoiceMatchResponse;
import com.eprocure.finance.presentation.response.InvoiceResponse;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class InvoicePresentationMapper {

    public ListInvoicesQuery toListQuery(
            UserPrincipal principal,
            InvoiceStatus status,
            UUID vendorId,
            UUID poId,
            Boolean overdueOnly,
            int page,
            int size) {
        return new ListInvoicesQuery(
                principal.getId(),
                status,
                vendorId,
                poId,
                overdueOnly,
                page,
                size);
    }

    public GetInvoiceQuery toGetQuery(UserPrincipal principal, UUID invoiceId) {
        return new GetInvoiceQuery(principal.getId(), invoiceId);
    }

    public CreateInvoiceCommand toCreateCommand(UserPrincipal principal, CreateInvoiceRequest request) {
        return new CreateInvoiceCommand(
                principal.getId(),
                request.invoiceNumber(),
                request.vendorId(),
                request.poId(),
                request.invoiceDate(),
                request.dueDate(),
                request.lineItems().stream()
                        .map(line -> new CreateInvoiceCommand.LineItem(
                                line.poLineItemId(),
                                line.description(),
                                line.quantity(),
                                line.unitPrice(),
                                line.taxRate()))
                        .toList());
    }

    public MatchInvoiceCommand toMatchCommand(UserPrincipal principal, UUID invoiceId) {
        return new MatchInvoiceCommand(principal.getId(), invoiceId);
    }

    public InvoiceResponse toResponse(InvoiceView view) {
        return new InvoiceResponse(
                view.id(),
                view.invoiceNumber(),
                new InvoiceResponse.VendorResponse(view.vendor().id(), view.vendor().name()),
                new InvoiceResponse.PurchaseOrderResponse(view.po().id(), view.po().poNumber()),
                view.lineItems().stream().map(this::toResponse).toList(),
                format(view.subtotal()),
                format(view.taxAmount()),
                format(view.totalAmount()),
                view.totalAmount().currency(),
                view.invoiceDate(),
                view.dueDate(),
                view.status(),
                toResponse(view.matchResult()),
                view.createdAt());
    }

    private InvoiceLineItemResponse toResponse(InvoiceLineItemView view) {
        return new InvoiceLineItemResponse(
                view.lineNumber(),
                view.poLineItemId(),
                view.description(),
                view.quantity().toPlainString(),
                format(view.unitPrice()),
                format(view.totalPrice()));
    }

    public InvoiceMatchResponse toResponse(InvoiceMatchResult result) {
        return new InvoiceMatchResponse(
                result.matchStatus(),
                new InvoiceMatchResponse.MatchResultResponse(
                        result.poMatchStatus(),
                        result.grMatchStatus(),
                        toPlainString(result.qtyVariance()),
                        format(result.priceVariance())),
                result.requiresManualReview());
    }

    private InvoiceResponse.MatchResultResponse toResponse(InvoiceView.MatchResultView view) {
        if (view == null) {
            return null;
        }
        return new InvoiceResponse.MatchResultResponse(
                view.poMatchStatus(),
                view.grMatchStatus(),
                toPlainString(view.qtyVariance()),
                format(view.priceVariance()),
                view.matchedAt());
    }

    private String format(Money money) {
        return money == null ? null : money.amount().toPlainString();
    }

    private String toPlainString(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
