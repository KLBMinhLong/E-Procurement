package com.eprocure.inventory.presentation.response;

import com.eprocure.inventory.domain.model.GoodsReceiptStatus;
import java.util.List;

public record CompleteGoodsReceiptResponse(
        GoodsReceiptStatus grStatus,
        int movementsCreated,
        List<StockUpdateResponse> updatedStocks) {

    public record StockUpdateResponse(String itemCode, String newQuantityOnHand) {
    }
}
