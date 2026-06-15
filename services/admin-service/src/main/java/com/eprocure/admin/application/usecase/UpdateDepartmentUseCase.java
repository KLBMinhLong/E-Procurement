package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.port.in.ManageDepartmentCommand;
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
    private final IdempotencyGuard idempotencyGuard;

    public UpdateDepartmentUseCase(IamDepartmentAdminPort iamDepartmentAdminPort, IdempotencyGuard idempotencyGuard) {
        this.iamDepartmentAdminPort = iamDepartmentAdminPort;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public DepartmentAdminView execute(ManageDepartmentCommand command, String idempotencyKey) {
        UUID key = idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start UpdateDepartment | actorId={} | departmentId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.departmentId()));
        DepartmentAdminView view = iamDepartmentAdminPort.updateDepartment(command, key);
        log.info("[ACTION] Complete UpdateDepartment | actorId={} | departmentId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.departmentId()));
        return view;
    }
}
