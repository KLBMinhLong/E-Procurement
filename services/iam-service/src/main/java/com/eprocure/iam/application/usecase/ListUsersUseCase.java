package com.eprocure.iam.application.usecase;

import com.eprocure.iam.application.port.in.ListUsersQuery;
import com.eprocure.iam.application.service.PageMeta;
import com.eprocure.iam.application.service.PageResult;
import com.eprocure.iam.application.service.UserSummaryView;
import com.eprocure.iam.application.service.UserViewAssembler;
import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.model.UserSearchCriteria;
import com.eprocure.iam.domain.repository.Page;
import com.eprocure.iam.domain.repository.UserRepository;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListUsersUseCase {
    private static final Logger log = LogManager.getLogger(ListUsersUseCase.class);
    private final UserRepository userRepository;
    private final UserViewAssembler userViewAssembler;

    public ListUsersUseCase(UserRepository userRepository, UserViewAssembler userViewAssembler) {
        this.userRepository = userRepository;
        this.userViewAssembler = userViewAssembler;
    }

    @Transactional(readOnly = true)
    public PageResult<UserSummaryView> execute(ListUsersQuery query) {
        log.info("[ACTION] Start ListUsers | page={} | size={}", query.page(), query.size());
        Page<User> page = userRepository.findPage(
                new UserSearchCriteria(query.status(), query.departmentId(), query.role(), query.query()),
                query.sortField(),
                query.sortDirection(),
                query.offset(),
                query.size());
        PageResult<UserSummaryView> result = new PageResult<>(
                page.items().stream().map(userViewAssembler::toSummary).toList(),
                PageMeta.of(page.totalElements(), query.page(), query.size(), query.sort()));
        log.info("[ACTION] Complete ListUsers | totalElements={}", page.totalElements());
        return result;
    }
}
