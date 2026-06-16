package com.eprocure.admin.presentation.mapper;

import com.eprocure.admin.application.port.in.DeactivateDepartmentCommand;
import com.eprocure.admin.application.port.in.ManageDepartmentCommand;
import com.eprocure.admin.application.service.AdminAuditContext;
import com.eprocure.admin.application.service.DepartmentAdminView;
import com.eprocure.admin.common.security.UserPrincipal;
import com.eprocure.admin.presentation.request.DepartmentRequest;
import com.eprocure.admin.presentation.response.DepartmentResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AdminDepartmentPresentationMapper {
    public ManageDepartmentCommand toCreateCommand(
            DepartmentRequest request,
            UserPrincipal principal,
            AdminAuditContext auditContext) {
        return toCommand(null, request, principal, auditContext);
    }

    public ManageDepartmentCommand toUpdateCommand(
            UUID departmentId,
            DepartmentRequest request,
            UserPrincipal principal,
            AdminAuditContext auditContext) {
        return toCommand(departmentId, request, principal, auditContext);
    }

    public DeactivateDepartmentCommand toDeactivateCommand(
            UUID departmentId,
            UserPrincipal principal,
            AdminAuditContext auditContext) {
        return new DeactivateDepartmentCommand(principal.getId(), departmentId, auditContext);
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

    public DepartmentResponse toResponse(DepartmentAdminView view) {
        return new DepartmentResponse(
                view.id(),
                view.code(),
                view.name(),
                view.parentId(),
                view.headUserId(),
                null,
                view.memberCount(),
                view.childCount(),
                view.deleted());
    }

    private ManageDepartmentCommand toCommand(
            UUID departmentId,
            DepartmentRequest request,
            UserPrincipal principal,
            AdminAuditContext auditContext) {
        return new ManageDepartmentCommand(
                principal.getId(),
                departmentId,
                request.code(),
                request.name(),
                request.parentId(),
                request.headUserId(),
                request.glAccountPrefix(),
                auditContext);
    }
}
