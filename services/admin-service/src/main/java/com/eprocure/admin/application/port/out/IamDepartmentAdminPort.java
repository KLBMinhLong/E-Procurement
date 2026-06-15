package com.eprocure.admin.application.port.out;

import com.eprocure.admin.application.port.in.DeactivateDepartmentCommand;
import com.eprocure.admin.application.port.in.ManageDepartmentCommand;
import com.eprocure.admin.application.service.DepartmentAdminView;
import java.util.UUID;

public interface IamDepartmentAdminPort {
    DepartmentAdminView createDepartment(ManageDepartmentCommand command, UUID idempotencyKey);

    DepartmentAdminView updateDepartment(ManageDepartmentCommand command, UUID idempotencyKey);

    DepartmentAdminView deactivateDepartment(DeactivateDepartmentCommand command, UUID idempotencyKey);
}
