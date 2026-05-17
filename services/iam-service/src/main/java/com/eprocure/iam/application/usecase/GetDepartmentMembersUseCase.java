package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.ListDepartmentMembersQuery;
import com.eprocure.iam.application.service.PageMeta;
import com.eprocure.iam.application.service.PageResult;
import com.eprocure.iam.application.service.UserSummaryView;
import com.eprocure.iam.application.service.UserViewAssembler;
import com.eprocure.iam.common.exception.BusinessException;
import com.eprocure.iam.common.exception.ErrorCode;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.repository.OrganizationRepository;
import com.eprocure.iam.domain.repository.Page;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetDepartmentMembersUseCase {
    private static final Logger log = LogManager.getLogger(GetDepartmentMembersUseCase.class);
    private final OrganizationRepository organizationRepository;
    private final UserViewAssembler userViewAssembler;

    public GetDepartmentMembersUseCase(
            OrganizationRepository organizationRepository,
            UserViewAssembler userViewAssembler) {
        this.organizationRepository = organizationRepository;
        this.userViewAssembler = userViewAssembler;
    }

    @Transactional(readOnly = true)
    public PageResult<UserSummaryView> execute(ListDepartmentMembersQuery query) {
        log.info("[ACTION] Start GetDepartmentMembers | departmentId={} | page={} | size={}",
                LogMaskingUtil.maskId(query.departmentId()),
                query.page(),
                query.size());
        if (organizationRepository.findDepartmentById(query.departmentId()).isEmpty()) {
            throw new BusinessException(ErrorCode.IAM_033);
        }
        Page<User> page = organizationRepository.findDepartmentMembers(query.departmentId(), query.offset(), query.size());
        PageResult<UserSummaryView> result = new PageResult<>(
                page.items().stream().map(userViewAssembler::toSummary).toList(),
                PageMeta.of(page.totalElements(), query.page(), query.size(), "fullName,asc"));
        log.info("[ACTION] Complete GetDepartmentMembers | departmentId={} | totalElements={}",
                LogMaskingUtil.maskId(query.departmentId()),
                page.totalElements());
        return result;
    }
}
