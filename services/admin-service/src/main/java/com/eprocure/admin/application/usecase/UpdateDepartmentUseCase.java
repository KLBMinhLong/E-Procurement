package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.port.in.ManageDepartmentCommand;
import com.eprocure.admin.application.port.out.AdminAuditLogWriterPort;
import com.eprocure.admin.application.port.out.IamDepartmentAdminPort;
import com.eprocure.admin.application.service.DepartmentAdminView;
import com.eprocure.admin.application.service.IdempotencyGuard;
import com.eprocure.admin.common.util.LogMaskingUtil;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateDepartmentUseCase {
    private static final Logger log = LogManager.getLogger(UpdateDepartmentUseCase.class);

    private final IamDepartmentAdminPort iamDepartmentAdminPort;
    private final AdminAuditLogWriterPort auditLogWriter;
    private final IdempotencyGuard idempotencyGuard;

    public UpdateDepartmentUseCase(
            IamDepartmentAdminPort iamDepartmentAdminPort,
            AdminAuditLogWriterPort auditLogWriter,
            IdempotencyGuard idempotencyGuard) {
        this.iamDepartmentAdminPort = iamDepartmentAdminPort;
        this.auditLogWriter = auditLogWriter;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public DepartmentAdminView execute(ManageDepartmentCommand command, String idempotencyKey) {
        UUID key = idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start UpdateDepartment | actorId={} | departmentId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.departmentId()));
        DepartmentAdminView view = iamDepartmentAdminPort.updateDepartment(command, key);
        auditLogWriter.recordDepartmentMutation("DEPARTMENT.UPDATED", view, command.auditContext());
        log.info("[ACTION] Complete UpdateDepartment | actorId={} | departmentId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.departmentId()));
        return view;
    }
}
