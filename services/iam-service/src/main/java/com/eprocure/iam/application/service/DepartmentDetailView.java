package com.eprocure.iam.application.service;

import java.util.List;
import java.util.UUID;

public record DepartmentDetailView(
        UUID id,
        String code,
        String name,
        UUID parentId,
        UserSummaryView headUser,
        long memberCount,
        List<DepartmentDetailView> children) {
    public DepartmentDetailView {
        children = List.copyOf(children);
    }
}
