package com.eprocure.admin.presentation.response;

public record CatalogCategoryResponse(
        String code,
        String name,
        String parentCode,
        boolean requiresSpecialApproval,
        String specialApproverRole,
        String requiresRfqAbove,
        boolean isCapex,
        long itemCount,
        boolean isDeleted) {
}
