package com.eprocure.inventory.presentation.mapper;

import com.eprocure.inventory.application.port.in.GetItemStockQuery;
import com.eprocure.inventory.application.port.in.ListStockMovementsQuery;
import com.eprocure.inventory.application.port.in.ListWarehouseStockQuery;
import com.eprocure.inventory.application.service.StockEntryView;
import com.eprocure.inventory.application.service.StockMovementView;
import com.eprocure.inventory.common.security.UserPrincipal;
import com.eprocure.inventory.domain.model.StockMovementType;
import com.eprocure.inventory.presentation.response.StockEntryResponse;
import com.eprocure.inventory.presentation.response.StockMovementResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StockPresentationMapper {

    public GetItemStockQuery toGetItemStockQuery(
            UserPrincipal principal,
            String itemCode,
            UUID warehouseId) {
        return new GetItemStockQuery(principal.getId(), itemCode, warehouseId);
    }

    public ListWarehouseStockQuery toListWarehouseStockQuery(
            UserPrincipal principal,
            UUID warehouseId,
            Boolean belowReorder,
            int page,
            int size) {
        return new ListWarehouseStockQuery(
                principal.getId(),
                warehouseId,
                belowReorder,
                page,
                size);
    }

    public ListStockMovementsQuery toListStockMovementsQuery(
            UserPrincipal principal,
            String itemCode,
            UUID warehouseId,
            StockMovementType movementType,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size) {
        return new ListStockMovementsQuery(
                principal.getId(),
                itemCode,
                warehouseId,
                movementType,
                fromDate,
                toDate,
                page,
                size);
    }

    public StockEntryResponse toResponse(StockEntryView view) {
        return new StockEntryResponse(
                view.itemCode(),
                view.itemName(),
                view.warehouseId(),
                view.warehouseName(),
                toPlainString(view.quantityOnHand()),
                view.unit(),
                toPlainString(view.reorderPoint()),
                view.belowReorder(),
                view.lastUpdated());
    }

    public StockMovementResponse toResponse(StockMovementView view) {
        return new StockMovementResponse(
                view.id(),
                view.itemCode(),
                view.itemName(),
                view.warehouseId(),
                view.movementType(),
                toPlainString(view.quantity()),
                view.unit(),
                toPlainString(view.balanceAfter()),
                view.sourceRefType(),
                view.sourceRefId(),
                new StockMovementResponse.PerformedByResponse(
                        view.performedBy().id(),
                        view.performedBy().fullName()),
                view.performedAt(),
                view.notes());
    }

    private String toPlainString(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
