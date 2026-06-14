package com.eprocure.inventory.presentation.mapper;

import com.eprocure.inventory.application.port.in.CreateItemCommand;
import com.eprocure.inventory.application.port.in.GetItemDetailQuery;
import com.eprocure.inventory.application.port.in.SearchItemsQuery;
import com.eprocure.inventory.application.port.in.UpdateItemCommand;
import com.eprocure.inventory.application.service.CatalogItemView;
import com.eprocure.inventory.common.security.UserPrincipal;
import com.eprocure.inventory.presentation.request.CreateItemRequest;
import com.eprocure.inventory.presentation.request.UpdateItemRequest;
import com.eprocure.inventory.presentation.response.CatalogItemResponse;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class ItemPresentationMapper {

    public SearchItemsQuery toSearchQuery(
            UserPrincipal principal,
            String query,
            String categoryCode,
            Boolean active,
            Boolean belowReorder,
            int page,
            int size) {
        return new SearchItemsQuery(
                principal.getId(),
                query,
                categoryCode,
                active,
                belowReorder,
                page,
                size);
    }

    public GetItemDetailQuery toGetQuery(UserPrincipal principal, String itemCode) {
        return new GetItemDetailQuery(principal.getId(), itemCode);
    }

    public CreateItemCommand toCreateCommand(UserPrincipal principal, CreateItemRequest request) {
        return new CreateItemCommand(
                principal.getId(),
                request.itemCode(),
                request.name(),
                request.description(),
                request.categoryCode(),
                request.unit(),
                request.unitPrice().amount(),
                request.unitPrice().currency(),
                request.preferredVendorId(),
                request.reorderPoint());
    }

    public UpdateItemCommand toUpdateCommand(
            UserPrincipal principal,
            String itemCode,
            UpdateItemRequest request) {
        return new UpdateItemCommand(
                principal.getId(),
                itemCode,
                request.name(),
                request.description(),
                request.unitPrice() == null ? null : request.unitPrice().amount(),
                request.unitPrice() == null ? null : request.unitPrice().currency(),
                request.preferredVendorId(),
                request.reorderPoint(),
                request.isActive());
    }

    public CatalogItemResponse toResponse(CatalogItemView view) {
        return new CatalogItemResponse(
                view.id(),
                view.itemCode(),
                view.name(),
                view.description(),
                view.categoryCode(),
                view.unit(),
                new CatalogItemResponse.MoneyResponse(
                        toPlainString(view.unitPrice()),
                        view.currency()),
                view.preferredVendorId(),
                toPlainString(view.reorderPoint()),
                view.active(),
                view.stockSummary().stream()
                        .map(stock -> new CatalogItemResponse.StockSummaryResponse(
                                stock.warehouseId(),
                                stock.warehouseName(),
                                toPlainString(stock.quantityOnHand()),
                                stock.unit()))
                        .toList());
    }

    private String toPlainString(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
