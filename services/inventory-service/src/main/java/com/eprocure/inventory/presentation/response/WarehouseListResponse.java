package com.eprocure.inventory.presentation.response;

import java.util.UUID;

public record WarehouseListResponse(UUID id, String code, String name) {
}
