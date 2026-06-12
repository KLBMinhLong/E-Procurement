package com.eprocure.inventory.presentation.mapper;

import com.eprocure.inventory.application.port.in.CompleteGoodsReceiptCommand;
import com.eprocure.inventory.application.port.in.CreateGoodsReceiptCommand;
import com.eprocure.inventory.application.port.in.GetGoodsReceiptQuery;
import com.eprocure.inventory.application.port.in.ListGoodsReceiptsQuery;
import com.eprocure.inventory.application.port.in.UpdateGoodsReceiptCommand;
import com.eprocure.inventory.application.service.CompleteGoodsReceiptResult;
import com.eprocure.inventory.application.service.GoodsReceiptLineItemView;
import com.eprocure.inventory.application.service.GoodsReceiptView;
import com.eprocure.inventory.common.security.UserPrincipal;
import com.eprocure.inventory.domain.model.GoodsReceiptStatus;
import com.eprocure.inventory.presentation.request.CreateGoodsReceiptRequest;
import com.eprocure.inventory.presentation.request.UpdateGoodsReceiptRequest;
import com.eprocure.inventory.presentation.response.CompleteGoodsReceiptResponse;
import com.eprocure.inventory.presentation.response.GoodsReceiptLineItemResponse;
import com.eprocure.inventory.presentation.response.GoodsReceiptResponse;
import com.eprocure.inventory.presentation.response.PoSnapshotResponse;
import com.eprocure.inventory.presentation.response.WarehouseKeeperSnapshotResponse;
import com.eprocure.inventory.presentation.response.WarehouseSnapshotResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GoodsReceiptPresentationMapper {

    public ListGoodsReceiptsQuery toListQuery(
            UserPrincipal principal,
            GoodsReceiptStatus status,
            UUID poId,
            UUID warehouseId,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size) {
        return new ListGoodsReceiptsQuery(
                principal.getId(),
                status,
                poId,
                warehouseId,
                fromDate,
                toDate,
                page,
                size);
    }

    public GetGoodsReceiptQuery toGetQuery(UserPrincipal principal, UUID goodsReceiptId) {
        return new GetGoodsReceiptQuery(principal.getId(), goodsReceiptId);
    }

    public CompleteGoodsReceiptCommand toCompleteCommand(UserPrincipal principal, UUID goodsReceiptId) {
        return new CompleteGoodsReceiptCommand(principal.getId(), goodsReceiptId);
    }

    public CreateGoodsReceiptCommand toCreateCommand(UserPrincipal principal, CreateGoodsReceiptRequest request) {
        return new CreateGoodsReceiptCommand(
                principal.getId(),
                principal.getFullName(),
                request.poId(),
                request.warehouseId(),
                request.receivedAt(),
                request.lineItems().stream()
                        .map(this::toCommandLine)
                        .toList(),
                request.notes());
    }

    public UpdateGoodsReceiptCommand toUpdateCommand(
            UserPrincipal principal,
            UUID goodsReceiptId,
            UpdateGoodsReceiptRequest request) {
        return new UpdateGoodsReceiptCommand(
                principal.getId(),
                goodsReceiptId,
                request.receivedAt(),
                request.lineItems().stream()
                        .map(this::toCommandLine)
                        .toList(),
                request.notes());
    }

    public GoodsReceiptResponse toResponse(GoodsReceiptView view) {
        return new GoodsReceiptResponse(
                view.id(),
                view.grNumber(),
                new PoSnapshotResponse(view.po().id(), view.po().poNumber()),
                new WarehouseSnapshotResponse(view.warehouse().id(), view.warehouse().name()),
                new WarehouseKeeperSnapshotResponse(view.warehouseKeeper().id(), view.warehouseKeeper().fullName()),
                view.receivedAt(),
                view.status(),
                view.lineItems().stream()
                        .map(this::toResponse)
                        .toList(),
                view.notes(),
                view.createdAt());
    }

    public CompleteGoodsReceiptResponse toResponse(CompleteGoodsReceiptResult result) {
        return new CompleteGoodsReceiptResponse(
                result.grStatus(),
                result.movementsCreated(),
                result.updatedStocks().stream()
                        .map(stock -> new CompleteGoodsReceiptResponse.StockUpdateResponse(
                                stock.itemCode(),
                                stock.newQuantityOnHand().toPlainString()))
                        .toList());
    }

    private CreateGoodsReceiptCommand.LineItem toCommandLine(CreateGoodsReceiptRequest.LineItem request) {
        return new CreateGoodsReceiptCommand.LineItem(
                request.poLineItemId(),
                request.receivedQuantity(),
                request.rejectedQuantity() == null ? BigDecimal.ZERO : request.rejectedQuantity(),
                request.rejectionReason(),
                request.lotNumber());
    }

    private UpdateGoodsReceiptCommand.LineItem toCommandLine(UpdateGoodsReceiptRequest.LineItem request) {
        return new UpdateGoodsReceiptCommand.LineItem(
                request.poLineItemId(),
                request.receivedQuantity(),
                request.rejectedQuantity() == null ? BigDecimal.ZERO : request.rejectedQuantity(),
                request.rejectionReason(),
                request.lotNumber());
    }

    private GoodsReceiptLineItemResponse toResponse(GoodsReceiptLineItemView view) {
        return new GoodsReceiptLineItemResponse(
                view.id(),
                view.poLineItemId(),
                view.itemCode(),
                view.itemName(),
                view.orderedQuantity().toPlainString(),
                view.receivedQuantity().toPlainString(),
                view.rejectedQuantity().toPlainString(),
                view.rejectionReason(),
                view.lotNumber(),
                view.unit());
    }
}
