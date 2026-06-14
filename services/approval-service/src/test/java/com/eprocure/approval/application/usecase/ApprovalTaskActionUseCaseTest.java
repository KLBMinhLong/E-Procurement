package com.eprocure.approval.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eprocure.approval.application.port.in.ApprovalTaskActionCommand;
import com.eprocure.approval.application.port.out.ApprovalStepAssignedEventPublisher;
import com.eprocure.approval.application.port.out.OrgApproverPort;
import com.eprocure.approval.application.port.out.PurchaseRequestStatusPort;
import com.eprocure.approval.application.service.ApprovalTaskActionView;
import com.eprocure.approval.application.service.IdempotencyService;
import com.eprocure.approval.application.service.ResolvedApprovalChainView;
import com.eprocure.approval.application.service.ResolvedApprovalChainView.ApproverView;
import com.eprocure.approval.application.service.ResolvedApprovalChainView.ResolvedApprovalStepView;
import com.eprocure.approval.common.exception.BusinessException;
import com.eprocure.approval.common.exception.ErrorCode;
import com.eprocure.approval.domain.event.ApprovalStepAssignedEvent;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApprovalTaskActionUseCaseTest {
    private static final UUID PR_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440100");
    private static final UUID DEPARTMENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID REQUESTER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID MANAGER_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID FINANCE_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID FORWARD_TO_ID = UUID.fromString("30000000-0000-0000-0000-000000000004");
    private static final String IDEMPOTENCY_KEY = "123e4567-e89b-42d3-a456-426614174000";
    private static final Instant NOW = Instant.parse("2026-06-01T01:00:00Z");

    private FakeApprovalProcessRepository processRepository;
    private FakeIdempotencyService idempotencyService;
    private FakeApprovalStepAssignedEventPublisher eventPublisher;
    private FakePurchaseRequestStatusPort purchaseRequestStatusPort;
    private FakeOrgApproverPort orgApproverPort;
    private ApprovalTaskActionUseCase useCase;

    @BeforeEach
    void setUp() {
        processRepository = new FakeApprovalProcessRepository();
        idempotencyService = new FakeIdempotencyService();
        eventPublisher = new FakeApprovalStepAssignedEventPublisher();
        purchaseRequestStatusPort = new FakePurchaseRequestStatusPort();
        orgApproverPort = new FakeOrgApproverPort();
        useCase = new ApprovalTaskActionUseCase(
                processRepository,
                idempotencyService,
                eventPublisher,
                purchaseRequestStatusPort,
                orgApproverPort,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void should_approve_current_step_and_assign_next_step_when_next_step_exists() {
        ApprovalProcess process = process();
        processRepository.process = process;
        UUID managerStepId = process.getSteps().get(0).getId();

        var result = useCase.execute(command(managerStepId, MANAGER_ID, ApprovalAction.APPROVE, null, null), IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(result.view().completed()).isFalse();
        assertThat(result.view().currentStepIndex()).isEqualTo(2);
        assertThat(result.view().nextSteps()).hasSize(1);
        assertThat(process.getCurrentStepIndex()).isEqualTo(2);
        assertThat(process.getSteps().get(0).getStatus()).isEqualTo(ApprovalStepStatus.APPROVED);
        assertThat(eventPublisher.events).hasSize(1);
        assertThat(eventPublisher.events.get(0).payload().approverId()).isEqualTo(FINANCE_ID);
        assertThat(purchaseRequestStatusPort.approved).isEmpty();
        assertThat(idempotencyService.savedResponse).isEqualTo(result.view());
    }

    @Test
    void should_complete_process_and_mark_pr_approved_when_final_step_is_approved() {
        ApprovalProcess process = process();
        process.approve(process.getSteps().get(0).getId(), MANAGER_ID, null, NOW.minusSeconds(60));
        processRepository.process = process;
        UUID financeStepId = process.getSteps().get(1).getId();

        var result = useCase.execute(command(financeStepId, FINANCE_ID, ApprovalAction.APPROVE, null, null), IDEMPOTENCY_KEY);

        assertThat(result.view().completed()).isTrue();
        assertThat(process.getStatus()).isEqualTo(ApprovalProcessStatus.COMPLETED);
        assertThat(purchaseRequestStatusPort.approved).hasSize(1);
        assertThat(purchaseRequestStatusPort.approved.get(0).purchaseRequestId()).isEqualTo(PR_ID);
        assertThat(eventPublisher.events).isEmpty();
    }

    @Test
    void should_reject_process_and_mark_pr_rejected_when_comment_is_valid() {
        ApprovalProcess process = process();
        processRepository.process = process;
        UUID managerStepId = process.getSteps().get(0).getId();

        var result = useCase.execute(
                command(managerStepId, MANAGER_ID, ApprovalAction.REJECT, "Gia cao hon thi truong can bao gia khac", null),
                IDEMPOTENCY_KEY);

        assertThat(result.view().completed()).isTrue();
        assertThat(process.getStatus()).isEqualTo(ApprovalProcessStatus.CANCELLED);
        assertThat(process.getSteps().get(0).getStatus()).isEqualTo(ApprovalStepStatus.REJECTED);
        assertThat(purchaseRequestStatusPort.rejected).hasSize(1);
    }

    @Test
    void should_throw_apr_005_when_reject_comment_is_too_short() {
        ApprovalProcess process = process();
        processRepository.process = process;
        UUID managerStepId = process.getSteps().get(0).getId();

        assertThatThrownBy(() -> useCase.execute(
                        command(managerStepId, MANAGER_ID, ApprovalAction.REJECT, "Qua dat", null),
                        IDEMPOTENCY_KEY))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.APR_005);
        assertThat(purchaseRequestStatusPort.rejected).isEmpty();
    }

    @Test
    void should_forward_task_when_target_is_eligible_for_same_role() {
        ApprovalProcess process = process();
        processRepository.process = process;
        UUID managerStepId = process.getSteps().get(0).getId();

        var result = useCase.execute(
                command(managerStepId, MANAGER_ID, ApprovalAction.FORWARD, "Chuyen de tranh xung dot loi ich", FORWARD_TO_ID),
                IDEMPOTENCY_KEY);

        assertThat(result.view().completed()).isFalse();
        assertThat(result.view().nextSteps()).hasSize(1);
        assertThat(processRepository.insertedSteps).hasSize(1);
        assertThat(processRepository.insertedSteps.get(0).getApproverId()).isEqualTo(FORWARD_TO_ID);
        assertThat(process.getSteps().get(0).getStatus()).isEqualTo(ApprovalStepStatus.FORWARDED);
        assertThat(eventPublisher.events).hasSize(1);
    }

    private ApprovalTaskActionCommand command(
            UUID taskId,
            UUID actorId,
            ApprovalAction action,
            String comment,
            UUID forwardToUserId) {
        return new ApprovalTaskActionCommand(taskId.toString(), actorId, action, comment, List.of(), forwardToUserId);
    }

    private ApprovalProcess process() {
        return ApprovalProcess.createForPurchaseRequest(
                PR_ID,
                "PR-2026-05-00001",
                "Mua thiet bi van phong",
                REQUESTER_ID,
                DEPARTMENT_ID,
                Money.vnd("12000000.0000"),
                PurchaseRequestPriority.NORMAL,
                "camunda-001",
                Map.of("prNumber", "PR-2026-05-00001"),
                new ResolvedApprovalChainView(
                        PR_ID,
                        UUID.fromString("550e8400-e29b-41d4-a716-446655441000"),
                        "VALUE_DEFAULT",
                        List.of("VALUE_DEFAULT"),
                        List.of(
                                step(1, "PR_APPROVE_L1", MANAGER_ID),
                                step(2, "PR_APPROVE_FINANCE", FINANCE_ID))),
                NOW);
    }

    private ResolvedApprovalStepView step(int index, String requiredPermission, UUID approverId) {
        return new ResolvedApprovalStepView(
                index,
                index,
                "VALUE_DEFAULT",
                requiredPermission,
                ApprovalStepType.SEQUENTIAL,
                24,
                NOW.plusSeconds(3600L * index),
                true,
                null,
                new ApproverView(
                        approverId,
                        "EMP-" + requiredPermission,
                        requiredPermission.toLowerCase(),
                        requiredPermission,
                        requiredPermission.toLowerCase() + "@eprocure.local",
                        DEPARTMENT_ID));
    }

    private static final class FakeApprovalProcessRepository implements ApprovalProcessRepository {
        private ApprovalProcess process;
        private final List<ApprovalStep> insertedSteps = new ArrayList<>();

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
            return Optional.ofNullable(process)
                    .filter(value -> value.findStep(stepId).isPresent());
        }

        @Override
        public Optional<ApprovalProcess> findRunningByCamundaTaskId(String camundaTaskId) {
            return Optional.empty();
        }

        @Override
        public Optional<ApprovalProcess> findLatestByEntity(ApprovalEntityType entityType, UUID entityId) {
            return Optional.empty();
        }

        @Override
        public List<ApprovalProcess> findRunningProcessesWithOverdueSteps(Instant now, int limit) {
            return List.of();
        }

        @Override
        public void updateProcessRuntime(ApprovalProcess process) {
            this.process = process;
        }

        @Override
        public void updateStep(ApprovalStep step) {
        }

        @Override
        public void insertStep(ApprovalStep step) {
            insertedSteps.add(step);
        }

        @Override
        public List<com.eprocure.approval.domain.repository.PendingTaskProjection> findPendingTasks(
                UUID approverId,
                String priority,
                String entityType,
                java.math.BigDecimal minAmount,
                Boolean isOverdue,
                String orderByColumn,
                String sortDirection,
                int offset,
                int limit) {
            return List.of();
        }

        @Override
        public long countPendingTasks(
                UUID approverId,
                String priority,
                String entityType,
                java.math.BigDecimal minAmount,
                Boolean isOverdue) {
            return 0;
        }
    }

    private static final class FakeIdempotencyService extends IdempotencyService {
        private ApprovalTaskActionView cached;
        private Object savedResponse;

        private FakeIdempotencyService() {
            super(null, null);
        }

        @Override
        public void verify(String idempotencyKey) {
        }

        @Override
        public <T> Optional<T> find(String operation, UUID actorId, String idempotencyKey, Class<T> type) {
            return Optional.ofNullable(cached).map(type::cast);
        }

        @Override
        public void save(String operation, UUID actorId, String idempotencyKey, Object response) {
            savedResponse = response;
        }
    }

    private static final class FakeApprovalStepAssignedEventPublisher implements ApprovalStepAssignedEventPublisher {
        private final List<ApprovalStepAssignedEvent> events = new ArrayList<>();

        @Override
        public void publish(ApprovalStepAssignedEvent event) {
            events.add(event);
        }
    }

    private static final class FakePurchaseRequestStatusPort implements PurchaseRequestStatusPort {
        private final List<ApprovalResultCommand> approved = new ArrayList<>();
        private final List<ApprovalResultCommand> rejected = new ArrayList<>();

        @Override
        public void markPendingApproval(MarkPendingApprovalCommand command) {
        }

        @Override
        public void markApproved(ApprovalResultCommand command) {
            approved.add(command);
        }

        @Override
        public void markRejected(ApprovalResultCommand command) {
            rejected.add(command);
        }

        @Override
        public void markChangesRequested(ApprovalResultCommand command) {
        }
    }

    private static final class FakeOrgApproverPort implements OrgApproverPort {
        @Override
        public List<ApproverCandidate> resolveApprovers(ResolveApproverQuery query) {
            if ("PR_APPROVE_L1".equals(query.requiredPermission())) {
                return List.of(
                        candidate(MANAGER_ID, "manager"),
                        candidate(FORWARD_TO_ID, "manager-forward"));
            }
            return List.of(candidate(FINANCE_ID, "finance"));
        }

        private ApproverCandidate candidate(UUID id, String username) {
            return new ApproverCandidate(
                    id,
                    "EMP-" + username,
                    username,
                    username,
                    username + "@eprocure.local",
                    DEPARTMENT_ID);
        }
    }
}
