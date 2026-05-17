package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.service.DepartmentDetailView;
import com.eprocure.iam.application.service.UserSummaryView;
import com.eprocure.iam.application.service.UserViewAssembler;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.Department;
import com.eprocure.iam.domain.repository.OrganizationRepository;
import com.eprocure.iam.domain.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetDepartmentTreeUseCase {
    private static final Logger log = LogManager.getLogger(GetDepartmentTreeUseCase.class);
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final UserViewAssembler userViewAssembler;

    public GetDepartmentTreeUseCase(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            UserViewAssembler userViewAssembler) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.userViewAssembler = userViewAssembler;
    }

    @Transactional(readOnly = true)
    public List<DepartmentDetailView> execute() {
        log.info("[ACTION] Start GetDepartmentTree");
        List<Department> departments = organizationRepository.findAllDepartments();
        Map<UUID, List<Department>> byParentId = departments.stream()
                .filter(department -> department.getParentId().isPresent())
                .collect(Collectors.groupingBy(department -> department.getParentId().orElseThrow()));
        List<DepartmentDetailView> tree = departments.stream()
                .filter(department -> department.getParentId().isEmpty())
                .sorted(Comparator.comparing(Department::getName))
                .map(department -> toDetail(department, byParentId))
                .toList();
        log.info("[ACTION] Complete GetDepartmentTree | rootCount={}", tree.size());
        return tree;
    }

    private DepartmentDetailView toDetail(Department department, Map<UUID, List<Department>> byParentId) {
        UserSummaryView headUser = department.getHeadUserId()
                .flatMap(userRepository::findById)
                .map(userViewAssembler::toSummary)
                .orElse(null);
        List<DepartmentDetailView> children = byParentId.getOrDefault(department.getId(), List.of()).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Department::getName))
                .map(child -> toDetail(child, byParentId))
                .toList();
        log.debug("[ACTION] Step GetDepartmentTree | departmentId={}", LogMaskingUtil.maskId(department.getId()));
        return new DepartmentDetailView(
                department.getId(),
                department.getCode(),
                department.getName(),
                department.getParentId().orElse(null),
                headUser,
                organizationRepository.countActiveMembersByDepartmentId(department.getId()),
                children);
    }
}
