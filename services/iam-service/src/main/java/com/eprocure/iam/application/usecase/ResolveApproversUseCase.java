package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.ResolveApproversQuery;
import com.eprocure.iam.application.service.UserSummaryView;
import com.eprocure.iam.application.service.UserViewAssembler;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.repository.OrganizationRepository;
import com.eprocure.iam.domain.repository.PermissionRepository;
import com.eprocure.iam.domain.repository.RoleRepository;
import com.eprocure.iam.domain.model.User;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResolveApproversUseCase {
    private static final Logger log = LogManager.getLogger(ResolveApproversUseCase.class);
    private static final int DEFAULT_LIMIT = 50;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserViewAssembler userViewAssembler;

    public ResolveApproversUseCase(
            OrganizationRepository organizationRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserViewAssembler userViewAssembler) {
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userViewAssembler = userViewAssembler;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryView> execute(ResolveApproversQuery query) {
        if (isBlank(query.roleCode()) && isBlank(query.permissionCode())) {
            throw new BusinessException(ErrorCode.IAM_005);
        }
        log.info("[ACTION] Start ResolveApprovers | roleCode={} | permissionCode={} | departmentId={} | requesterId={}",
                query.roleCode(),
                query.permissionCode(),
                LogMaskingUtil.maskId(query.departmentId()),
                LogMaskingUtil.maskId(query.requesterId()));
        if (!isBlank(query.roleCode()) && !roleRepository.findExistingCodes(Set.of(query.roleCode())).contains(query.roleCode())) {
            throw new BusinessException(ErrorCode.IAM_031);
        }
        if (!isBlank(query.permissionCode()) && !permissionRepository.findExistingCodes(Set.of(query.permissionCode())).contains(query.permissionCode())) {
            throw new BusinessException(ErrorCode.IAM_005);
        }
        if (query.departmentId() != null && organizationRepository.findDepartmentById(query.departmentId()).isEmpty()) {
            throw new BusinessException(ErrorCode.IAM_033);
        }
        List<User> candidates = isBlank(query.permissionCode())
                ? organizationRepository.findApprovers(query.roleCode(), query.departmentId(), query.requesterId(), DEFAULT_LIMIT)
                : organizationRepository.findApproversByPermission(query.permissionCode(), query.departmentId(), query.requesterId(), DEFAULT_LIMIT);
        List<UserSummaryView> approvers = candidates
                .stream()
                .map(userViewAssembler::toSummary)
                .toList();
        if (approvers.isEmpty()) {
            throw new BusinessException(ErrorCode.IAM_034);
        }
        log.info("[ACTION] Complete ResolveApprovers | roleCode={} | permissionCode={} | count={}",
                query.roleCode(),
                query.permissionCode(),
                approvers.size());
        return approvers;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
