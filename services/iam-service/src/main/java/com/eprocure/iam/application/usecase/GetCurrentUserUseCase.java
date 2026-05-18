package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.service.CurrentUserView;
import com.eprocure.iam.application.service.DepartmentView;
import com.eprocure.iam.application.service.PermissionResolutionService;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.Department;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.DepartmentRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;

@Service
public class GetCurrentUserUseCase {
    private static final Logger log = LogManager.getLogger(GetCurrentUserUseCase.class);
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PermissionResolutionService permissionResolutionService;

    public GetCurrentUserUseCase(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            PermissionResolutionService permissionResolutionService) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.permissionResolutionService = permissionResolutionService;
    }

    public CurrentUserView execute(UUID userId) {
        log.info("[ACTION] Start GetCurrentUser | userId={}", LogMaskingUtil.maskId(userId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IAM_030));
        DepartmentView department = departmentRepository.findById(user.getDepartmentId())
                .map(this::toDepartmentView)
                .orElse(null);
        Set<String> roles = userRepository.findRoleCodesByUserId(user.getId());
        CurrentUserView view = new CurrentUserView(
                user.getId(),
                user.getEmployeeCode(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone().orElse(null),
                user.getAvatarUrl().orElse(null),
                department,
                user.getOrgNodeId().orElse(null),
                roles,
                permissionResolutionService.resolveByRoleCodes(roles),
                user.getStatus(),
                user.isTwoFactorEnabled(),
                user.getLastLoginAt().orElse(null),
                user.getCreatedAt());
        log.info("[ACTION] Complete GetCurrentUser | userId={}", LogMaskingUtil.maskId(userId));
        return view;
    }

    private DepartmentView toDepartmentView(Department department) {
        return new DepartmentView(department.getId(), department.getCode(), department.getName());
    }
}
