package com.eprocure.approval.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.approval.application.port.out.UserResolverPort;
import com.eprocure.approval.application.service.ApprovalProcessDetailAssembler;
import com.eprocure.approval.common.exception.BusinessException;
import com.eprocure.approval.common.exception.ErrorCode;
import com.eprocure.approval.domain.model.ApprovalAction;
import com.eprocure.approval.domain.model.ApprovalEntityType;
import com.eprocure.approval.domain.model.ApprovalProcess;
import com.eprocure.approval.domain.model.ApprovalProcessStatus;
import com.eprocure.approval.domain.model.ApprovalStep;
import com.eprocure.approval.domain.model.ApprovalStepStatus;
import com.eprocure.approval.domain.model.ApprovalStepType;
import com.eprocure.approval.domain.model.PurchaseRequestPriority;
import com.eprocure.approval.domain.model.vo.Money;
import com.eprocure.approval.domain.repository.ApprovalProcessRepository;
import com.eprocure.approval.domain.repository.PendingTaskProjection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetApprovalProcessUseCaseTest {
    private static final UUID PROCESS_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PR_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440100");
    private static final UUID DEPARTMENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID OTHER_DEPARTMENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final UUID REQUESTER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID MANAGER_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID UNRELATED_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final Instant NOW = Instant.parse("2026-06-01T01:00:00Z");

    private FakeApprovalProcessRepository processRepository;
    private GetApprovalProcessUseCase useCase;

    @BeforeEach
    void setUp() {
        processRepository = new FakeApprovalProcessRepository();
        useCase = new GetApprovalProcessUseCase(
                processRepository,
                new ApprovalProcessDetailAssembler(new FakeUserResolverPort()));
    }

    @Test
    void should_return_process_when_requester_has_pr_view_own() {
        processRepository.process = process();

        var result = useCase.execute(
                "PURCHASE_REQUEST",
                PR_ID,
                REQUESTER_ID,
                DEPARTMENT_ID,
                Set.of("PR_VIEW_OWN"));

        assertThat(result.id()).isEqualTo(PROCESS_ID);
        assertThat(result.entityId()).isEqualTo(PR_ID);
        assertThat(result.steps()).hasSize(1);
        assertThat(result.steps().get(0).requiredPermission()).isEqualTo("PR_APPROVE_L1");
        assertThat(result.steps().get(0).approver().fullName()).isEqualTo("Manager User");
    }

    @Test
    void should_return_process_when_actor_is_assigned_approver() {
        processRepository.process = process();

        var result = useCase.execute(
                "purchase_request",
                PR_ID,
                MANAGER_ID,
                OTHER_DEPARTMENT_ID,
                Set.of("PR_APPROVE_L1"));

        assertThat(result.currentStepIndex()).isEqualTo(1);
        assertThat(result.steps().get(0).status()).isEqualTo("PENDING");
    }

    @Test
    void should_throw_iam_004_when_pr_view_own_actor_is_not_requester() {
        processRepository.process = process();

        assertThatThrownBy(() -> useCase.execute(
                        "PURCHASE_REQUEST",
                        PR_ID,
                        UNRELATED_ID,
                        OTHER_DEPARTMENT_ID,
                        Set.of("PR_VIEW_OWN")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IAM_004);
    }

    @Test
    void should_throw_apr_011_when_process_is_missing() {
        assertThatThrownBy(() -> useCase.execute(
                        "PURCHASE_REQUEST",
                        PR_ID,
                        REQUESTER_ID,
                        DEPARTMENT_ID,
                        Set.of("PR_VIEW_OWN")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.APR_011);
    }

    private ApprovalProcess process() {
        ApprovalStep step = ApprovalStep.pending(
                PROCESS_ID,
                1,
                ApprovalStepType.SEQUENTIAL,
                "PR_APPROVE_L1",
                MANAGER_ID,
                NOW.plusSeconds(3600),
                NOW);
        return ApprovalProcess.restore(
                PROCESS_ID,
                ApprovalEntityType.PURCHASE_REQUEST,
                PR_ID,
                "PR-2026-05-00001",
                "Mua thiet bi van phong",
                REQUESTER_ID,
                DEPARTMENT_ID,
                Money.vnd("12000000.0000"),
                PurchaseRequestPriority.NORMAL,
                "camunda-001",
                ApprovalProcessStatus.RUNNING,
                1,
                Map.of("prNumber", "PR-2026-05-00001"),
                NOW,
                null,
                List.of(step));
    }

    private static final class FakeApprovalProcessRepository implements ApprovalProcessRepository {
        private ApprovalProcess process;

        @Override
        public boolean existsRunningByEntity(ApprovalEntityType entityType, UUID entityId) {
            return false;
        }

        @Override
        public void save(ApprovalProcess process) {
            this.process = process;
        }

        @Override
        public Optional<ApprovalProcess> findRunningByStepId(UUID stepId) {
            return Optional.empty();
        }

        @Override
        public Optional<ApprovalProcess> findRunningByCamundaTaskId(String camundaTaskId) {
            return Optional.empty();
        }

        @Override
        public Optional<ApprovalProcess> findLatestByEntity(ApprovalEntityType entityType, UUID entityId) {
            if (process == null || process.getEntityType() != entityType || !process.getEntityId().equals(entityId)) {
                return Optional.empty();
            }
            return Optional.of(process);
        }

        @Override
        public List<ApprovalProcess> findRunningProcessesWithOverdueSteps(Instant now, int limit) {
            return List.of();
        }

        @Override
        public void updateProcessRuntime(ApprovalProcess process) {
        }

        @Override
        public void updateStep(ApprovalStep step) {
        }

        @Override
        public void insertStep(ApprovalStep step) {
        }

        @Override
        public List<PendingTaskProjection> findPendingTasks(
                UUID userId,
                String priority,
                String entityType,
                BigDecimal minAmount,
                Boolean isOverdue,
                String orderByColumn,
                String sortDirection,
                int offset,
                int limit) {
            return List.of();
        }

        @Override
        public long countPendingTasks(
                UUID userId,
                String priority,
                String entityType,
                BigDecimal minAmount,
                Boolean isOverdue) {
            return 0;
        }
    }

    private static final class FakeUserResolverPort implements UserResolverPort {
        @Override
        public Optional<UserSummary> getUserById(UUID userId) {
            if (MANAGER_ID.equals(userId)) {
                return Optional.of(new UserSummary(MANAGER_ID, "Manager User", "Operations"));
            }
            return Optional.empty();
        }
    }
}
