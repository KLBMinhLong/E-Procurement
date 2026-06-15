package com.eprocure.admin.application.service;

public record CatalogCategoryAdminView(
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
