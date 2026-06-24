package com.eprocure.admin.presentation.response;

public record ServiceConfigSummaryResponse(
        int totalServices,
        int upCount,
        int downCount,
        int degradedCount,
        int unknownCount) {
}
