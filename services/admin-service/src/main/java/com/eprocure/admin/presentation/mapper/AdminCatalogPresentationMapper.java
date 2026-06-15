package com.eprocure.admin.presentation.mapper;

import com.eprocure.admin.application.port.in.DeactivateCatalogCategoryCommand;
import com.eprocure.admin.application.port.in.ManageCatalogCategoryCommand;
import com.eprocure.admin.application.service.CatalogCategoryAdminView;
import com.eprocure.admin.common.security.UserPrincipal;
import com.eprocure.admin.presentation.request.CatalogCategoryRequest;
import com.eprocure.admin.presentation.response.CatalogCategoryResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AdminCatalogPresentationMapper {

    public ManageCatalogCategoryCommand toCreateCommand(CatalogCategoryRequest request, UserPrincipal principal) {
        return toCommand(request.code(), request, principal);
    }

    public ManageCatalogCategoryCommand toUpdateCommand(String code, CatalogCategoryRequest request, UserPrincipal principal) {
        return toCommand(code, request, principal);
    }

    public DeactivateCatalogCategoryCommand toDeactivateCommand(String code, UserPrincipal principal) {
        return new DeactivateCatalogCategoryCommand(principal.getId(), code);
    }

    public List<CatalogCategoryResponse> toResponseList(List<CatalogCategoryAdminView> categories) {
        return categories.stream().map(this::toResponse).toList();
    }

    public CatalogCategoryResponse toResponse(CatalogCategoryAdminView category) {
        return new CatalogCategoryResponse(
                category.code(),
                category.name(),
                category.parentCode(),
                category.requiresSpecialApproval(),
                category.specialApproverRole(),
                category.requiresRfqAbove(),
                category.isCapex(),
                category.itemCount(),
                category.isDeleted());
    }

    private ManageCatalogCategoryCommand toCommand(String code, CatalogCategoryRequest request, UserPrincipal principal) {
        return new ManageCatalogCategoryCommand(
                principal.getId(),
                code,
                request.name(),
                request.parentCode(),
                request.requiresSpecialApproval(),
                request.specialApproverRole(),
                request.requiresRfqAbove(),
                Boolean.TRUE.equals(request.isCapex()));
    }
}
