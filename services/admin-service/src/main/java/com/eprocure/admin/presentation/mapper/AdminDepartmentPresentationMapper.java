package com.eprocure.admin.presentation.mapper;

import com.eprocure.admin.application.port.in.DeactivateDepartmentCommand;
import com.eprocure.admin.application.port.in.ManageDepartmentCommand;
import com.eprocure.admin.application.service.DepartmentAdminView;
import com.eprocure.admin.common.security.UserPrincipal;
import com.eprocure.admin.presentation.request.DepartmentRequest;
import com.eprocure.admin.presentation.response.DepartmentResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AdminDepartmentPresentationMapper {
    public ManageDepartmentCommand toCreateCommand(DepartmentRequest request, UserPrincipal principal) {
        return toCommand(null, request, principal);
    }

    public ManageDepartmentCommand toUpdateCommand(UUID departmentId, DepartmentRequest request, UserPrincipal principal) {
        return toCommand(departmentId, request, principal);
    }

    public DeactivateDepartmentCommand toDeactivateCommand(UUID departmentId, UserPrincipal principal) {
        return new DeactivateDepartmentCommand(principal.getId(), departmentId);
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

    private ManageDepartmentCommand toCommand(UUID departmentId, DepartmentRequest request, UserPrincipal principal) {
        return new ManageDepartmentCommand(
                principal.getId(),
                departmentId,
                request.code(),
                request.name(),
                request.parentId(),
                request.headUserId(),
                request.glAccountPrefix());
    }
}
