package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.DeactivateDepartmentCommand;
import com.eprocure.iam.application.service.DepartmentAdminView;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.Department;
import com.eprocure.iam.domain.repository.DepartmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeactivateDepartmentUseCase {
    private static final Logger log = LogManager.getLogger(DeactivateDepartmentUseCase.class);

    private final DepartmentRepository departmentRepository;
    private final IdempotencyGuard idempotencyGuard;
    private final Clock clock;

    public DeactivateDepartmentUseCase(
            DepartmentRepository departmentRepository,
            IdempotencyGuard idempotencyGuard,
            Clock clock) {
        this.departmentRepository = departmentRepository;
        this.idempotencyGuard = idempotencyGuard;
        this.clock = clock;
    }

    @Transactional
    public DepartmentAdminView execute(DeactivateDepartmentCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyGuard.verify(idempotencyKey);
        Department department = departmentRepository.findById(command.departmentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_033));
        if (departmentRepository.countActiveMembersByDepartmentId(command.departmentId()) > 0
                || departmentRepository.countActiveChildrenByDepartmentId(command.departmentId()) > 0) {
            throw new BusinessException(ErrorCode.IAM_037);
        }

        log.info("[ACTION] Start DeactivateDepartment | actorId={} | departmentId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.departmentId()));
        Instant now = Instant.now(clock);
        department.deactivate(now);
        departmentRepository.deactivate(command.departmentId(), command.actorId(), now);
        Department deleted = departmentRepository.findByIdIncludingInactive(command.departmentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_033));
        DepartmentAdminView view = toView(deleted);
        log.info("[ACTION] Complete DeactivateDepartment | actorId={} | departmentId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.departmentId()));
        return view;
    }

    private DepartmentAdminView toView(Department department) {
        return new DepartmentAdminView(
                department.getId(),
                department.getCode(),
                department.getName(),
                department.getParentId().orElse(null),
                department.getHeadUserId().orElse(null),
                departmentRepository.countActiveMembersByDepartmentId(department.getId()),
                departmentRepository.countActiveChildrenByDepartmentId(department.getId()),
                department.isDeleted());
    }
}
