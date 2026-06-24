package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.ManageDepartmentCommand;
import com.eprocure.iam.application.service.DepartmentAdminView;
import com.eprocure.iam.application.service.IdempotencyGuard;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.Department;
import com.eprocure.iam.domain.model.UserStatus;
import com.eprocure.iam.domain.repository.DepartmentRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateDepartmentUseCase {
    private static final Logger log = LogManager.getLogger(UpdateDepartmentUseCase.class);
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,19}$");

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final IdempotencyGuard idempotencyGuard;
    private final Clock clock;

    public UpdateDepartmentUseCase(
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            IdempotencyGuard idempotencyGuard,
            Clock clock) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.idempotencyGuard = idempotencyGuard;
        this.clock = clock;
    }

    @Transactional
    public DepartmentAdminView execute(ManageDepartmentCommand command, String idempotencyKey) {
        Objects.requireNonNull(command, "command must not be null");
        idempotencyGuard.verify(idempotencyKey);
        Department department = departmentRepository.findById(command.departmentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_033));
        String code = normalizeCode(command.code());
        String name = normalizeName(command.name());
        validateReferences(command.departmentId(), command.parentId(), command.headUserId());
        if (departmentRepository.existsActiveByCodeExceptId(code, command.departmentId())) {
            throw new BusinessException(ErrorCode.IAM_009);
        }

        log.info("[ACTION] Start UpdateDepartment | actorId={} | departmentId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.departmentId()));
        department.update(code, name, command.parentId(), command.headUserId(), Instant.now(clock));
        departmentRepository.update(department, command.actorId());
        Department updated = departmentRepository.findById(command.departmentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_033));
        DepartmentAdminView view = toView(updated);
        log.info("[ACTION] Complete UpdateDepartment | actorId={} | departmentId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(command.departmentId()));
        return view;
    }

    private void validateReferences(UUID departmentId, UUID parentId, UUID headUserId) {
        if (parentId != null) {
            if (parentId.equals(departmentId)) {
                throw new BusinessException(ErrorCode.IAM_005);
            }
            if (departmentRepository.findById(parentId).isEmpty()) {
                throw new BusinessException(ErrorCode.IAM_033);
            }
            if (departmentRepository.isDescendant(parentId, departmentId)) {
                throw new BusinessException(ErrorCode.IAM_005);
            }
        }
        if (headUserId != null) {
            var headUser = userRepository.findById(headUserId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));
            if (headUser.getStatus() != UserStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.IAM_005);
            }
        }
    }

    private String normalizeCode(String code) {
        if (code == null) {
            throw new BusinessException(ErrorCode.IAM_005);
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.IAM_005);
        }
        return normalized;
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 200) {
            throw new BusinessException(ErrorCode.IAM_005);
        }
        return name.trim();
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
