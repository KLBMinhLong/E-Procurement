package com.eprocure.admin.presentation.mapper;

import com.eprocure.admin.application.port.in.DeactivateCatalogCategoryCommand;
import com.eprocure.admin.application.port.in.ManageCatalogCategoryCommand;
import com.eprocure.admin.application.service.AdminAuditContext;
import com.eprocure.admin.application.service.CatalogCategoryAdminView;
import com.eprocure.admin.common.security.UserPrincipal;
import com.eprocure.admin.presentation.request.CatalogCategoryRequest;
import com.eprocure.admin.presentation.response.CatalogCategoryResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AdminCatalogPresentationMapper {

    public ManageCatalogCategoryCommand toCreateCommand(
            CatalogCategoryRequest request,
            UserPrincipal principal,
            AdminAuditContext auditContext) {
        return toCommand(request.code(), request, principal, auditContext);
    }

    public ManageCatalogCategoryCommand toUpdateCommand(
            String code,
            CatalogCategoryRequest request,
            UserPrincipal principal,
            AdminAuditContext auditContext) {
        return toCommand(code, request, principal, auditContext);
    }

    public DeactivateCatalogCategoryCommand toDeactivateCommand(
            String code,
            UserPrincipal principal,
            AdminAuditContext auditContext) {
        return new DeactivateCatalogCategoryCommand(principal.getId(), code, auditContext);
    }

    public AdminAuditContext toAuditContext(UserPrincipal principal, HttpServletRequest request, String requestId) {
        return new AdminAuditContext(
                principal.getId(),
                principal.getFullName(),
                principal.getPermissions().stream().sorted().toList(),
                Optional.ofNullable(request.getRemoteAddr()),
                Optional.ofNullable(request.getMethod()),
                Optional.ofNullable(request.getRequestURI()),
                Optional.ofNullable(requestId));
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

    private ManageCatalogCategoryCommand toCommand(
            String code,
            CatalogCategoryRequest request,
            UserPrincipal principal,
            AdminAuditContext auditContext) {
        return new ManageCatalogCategoryCommand(
                principal.getId(),
                code,
                request.name(),
                request.parentCode(),
                request.requiresSpecialApproval(),
                request.specialApproverRole(),
                request.requiresRfqAbove(),
                Boolean.TRUE.equals(request.isCapex()),
                auditContext);
    }
}
