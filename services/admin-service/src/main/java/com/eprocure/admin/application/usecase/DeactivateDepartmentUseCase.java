package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.port.in.DeactivateDepartmentCommand;
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
public class DeactivateDepartmentUseCase {
    private static final Logger log = LogManager.getLogger(DeactivateDepartmentUseCase.class);

    private final IamDepartmentAdminPort iamDepartmentAdminPort;
    private final AdminAuditLogWriterPort auditLogWriter;
    private final IdempotencyGuard idempotencyGuard;

    public DeactivateDepartmentUseCase(
            IamDepartmentAdminPort iamDepartmentAdminPort,
            AdminAuditLogWriterPort auditLogWriter,
            IdempotencyGuard idempotencyGuard) {
        this.iamDepartmentAdminPort = iamDepartmentAdminPort;
        this.auditLogWriter = auditLogWriter;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public DepartmentAdminView execute(DeactivateDepartmentCommand command, String idempotencyKey) {
        UUID key = idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start DeactivateDepartment | actorId={} | departmentId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.departmentId()));
        DepartmentAdminView view = iamDepartmentAdminPort.deactivateDepartment(command, key);
        auditLogWriter.recordDepartmentMutation("DEPARTMENT.DEACTIVATED", view, command.auditContext());
        log.info("[ACTION] Complete DeactivateDepartment | actorId={} | departmentId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.departmentId()));
        return view;
    }
}
