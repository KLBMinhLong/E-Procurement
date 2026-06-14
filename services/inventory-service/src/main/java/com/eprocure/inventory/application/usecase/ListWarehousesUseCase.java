package com.eprocure.inventory.application.usecase;

import com.eprocure.inventory.infrastructure.persistence.entity.WarehouseListDbEntity;
import com.eprocure.inventory.infrastructure.persistence.mapper.GoodsReceiptMapper;
import com.eprocure.inventory.presentation.response.WarehouseListResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListWarehousesUseCase {
    private final GoodsReceiptMapper goodsReceiptMapper;

    public ListWarehousesUseCase(GoodsReceiptMapper goodsReceiptMapper) {
        this.goodsReceiptMapper = goodsReceiptMapper;
    }

    @Transactional(readOnly = true)
    public List<WarehouseListResponse> execute() {
        return goodsReceiptMapper.findActiveWarehouses().stream()
                .map(this::toResponse)
                .toList();
    }

    private WarehouseListResponse toResponse(WarehouseListDbEntity entity) {
        return new WarehouseListResponse(entity.getId(), entity.getCode(), entity.getName());
    }
}
