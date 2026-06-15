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
public class CreateDepartmentUseCase {
    private static final Logger log = LogManager.getLogger(CreateDepartmentUseCase.class);

    private final IamDepartmentAdminPort iamDepartmentAdminPort;
    private final IdempotencyGuard idempotencyGuard;

    public CreateDepartmentUseCase(IamDepartmentAdminPort iamDepartmentAdminPort, IdempotencyGuard idempotencyGuard) {
        this.iamDepartmentAdminPort = iamDepartmentAdminPort;
        this.idempotencyGuard = idempotencyGuard;
    }

    @Transactional
    public DepartmentAdminView execute(ManageDepartmentCommand command, String idempotencyKey) {
        UUID key = idempotencyGuard.verify(idempotencyKey);
        log.info("[ACTION] Start CreateDepartment | actorId={} | code={}",
                LogMaskingUtil.maskId(command.actorId()),
                command.code());
        DepartmentAdminView view = iamDepartmentAdminPort.createDepartment(command, key);
        log.info("[ACTION] Complete CreateDepartment | actorId={} | departmentId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(view.id()));
        return view;
    }
}
