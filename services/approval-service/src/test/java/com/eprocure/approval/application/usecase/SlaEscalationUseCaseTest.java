package com.eprocure.approval.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.approval.application.port.out.ApprovalSlaBreachedEventPublisher;
import com.eprocure.approval.application.port.out.OrgApproverPort;
import com.eprocure.approval.application.service.ResolvedApprovalChainView;
import com.eprocure.approval.application.service.ResolvedApprovalChainView.ApproverView;
import com.eprocure.approval.application.service.ResolvedApprovalChainView.ResolvedApprovalStepView;
import com.eprocure.approval.domain.event.ApprovalSlaBreachedEvent;
import com.eprocure.approval.domain.model.ApprovalEntityType;
import com.eprocure.approval.domain.model.ApprovalProcess;
import com.eprocure.approval.domain.model.ApprovalStep;
import com.eprocure.approval.domain.model.ApprovalStepStatus;
import com.eprocure.approval.domain.model.ApprovalStepType;
import com.eprocure.approval.domain.model.PurchaseRequestPriority;
import com.eprocure.approval.domain.model.vo.Money;
import com.eprocure.approval.domain.repository.ApprovalProcessRepository;
import com.eprocure.approval.domain.repository.PendingTaskProjection;
import java.math.BigDecimal;
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

class SlaEscalationUseCaseTest {
    private static final UUID PR_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440100");
    private static final UUID DEPARTMENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID REQUESTER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID MANAGER_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID ESCALATION_TARGET_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final Instant NOW = Instant.parse("2026-06-01T05:00:00Z");
    private static final Instant OVERDUE_DEADLINE = Instant.parse("2026-06-01T04:00:00Z");

    private FakeApprovalProcessRepository processRepository;
    private FakeOrgApproverPort orgApproverPort;
    private FakeApprovalSlaBreachedEventPublisher eventPublisher;
    private SlaEscalationUseCase useCase;

    @BeforeEach
    void setUp() {
        processRepository = new FakeApprovalProcessRepository();
        orgApproverPort = new FakeOrgApproverPort();
        eventPublisher = new FakeApprovalSlaBreachedEventPublisher();
        useCase = new SlaEscalationUseCase(
                processRepository,
                orgApproverPort,
                eventPublisher,
                Clock.fixed(NOW, ZoneOffset.UTC),
                50);
    }

    @Test
    void should_escalate_overdue_pending_step_when_alternate_approver_exists() {
        ApprovalProcess process = process(OVERDUE_DEADLINE);
        processRepository.processes = List.of(process);
        ApprovalStep step = process.getSteps().get(0);

        var result = useCase.execute();

        assertThat(result.scannedProcesses()).isEqualTo(1);
        assertThat(result.escalatedSteps()).isEqualTo(1);
        assertThat(step.getStatus()).isEqualTo(ApprovalStepStatus.PENDING);
        assertThat(step.isEscalated()).isTrue();
        assertThat(step.getEscalatedFrom()).contains(MANAGER_ID);
        assertThat(step.getApproverId()).isEqualTo(ESCALATION_TARGET_ID);
        assertThat(processRepository.updatedSteps).containsExactly(step);
        assertThat(eventPublisher.events).hasSize(1);
        assertThat(eventPublisher.events.get(0).payload().breachedApproverId()).isEqualTo(MANAGER_ID);
        assertThat(eventPublisher.events.get(0).payload().escalatedToApproverId()).isEqualTo(ESCALATION_TARGET_ID);
        assertThat(eventPublisher.events.get(0).payload().reassigned()).isTrue();
    }

    @Test
    void should_notify_original_approver_when_no_alternate_escalation_target_exists() {
        ApprovalProcess process = process(OVERDUE_DEADLINE);
        processRepository.processes = List.of(process);
        orgApproverPort.candidates = List.of(candidate(MANAGER_ID, "manager"));
        ApprovalStep step = process.getSteps().get(0);

        var result = useCase.execute();

        assertThat(result.escalatedSteps()).isEqualTo(1);
        assertThat(step.isEscalated()).isTrue();
        assertThat(step.getEscalatedFrom()).contains(MANAGER_ID);
        assertThat(step.getApproverId()).isEqualTo(MANAGER_ID);
        assertThat(eventPublisher.events).hasSize(1);
        assertThat(eventPublisher.events.get(0).payload().reassigned()).isFalse();
    }

    @Test
    void should_skip_step_when_already_escalated() {
        ApprovalProcess process = process(OVERDUE_DEADLINE);
        ApprovalStep step = process.getSteps().get(0);
        step.escalateTo(ESCALATION_TARGET_ID, NOW.minusSeconds(60));
        processRepository.processes = List.of(process);

        var result = useCase.execute();

        assertThat(result.escalatedSteps()).isZero();
        assertThat(processRepository.updatedSteps).isEmpty();
        assertThat(eventPublisher.events).isEmpty();
    }

    private ApprovalProcess process(Instant slaDeadline) {
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
                        List.of(step(slaDeadline))),
                NOW.minusSeconds(7200));
    }

    private ResolvedApprovalStepView step(Instant slaDeadline) {
        return new ResolvedApprovalStepView(
                1,
                1,
                "VALUE_DEFAULT",
                "PR_APPROVE_L1",
                ApprovalStepType.SEQUENTIAL,
                2,
                slaDeadline,
                true,
                null,
                new ApproverView(
                        MANAGER_ID,
                        "EMP-MANAGER",
                        "manager",
                        "Manager",
                        "manager@eprocure.local",
                        DEPARTMENT_ID));
    }

    private static OrgApproverPort.ApproverCandidate candidate(UUID id, String username) {
        return new OrgApproverPort.ApproverCandidate(
                id,
                "EMP-" + username,
                username,
                username,
                username + "@eprocure.local",
                DEPARTMENT_ID);
    }

    private static final class FakeApprovalProcessRepository implements ApprovalProcessRepository {
        private List<ApprovalProcess> processes = List.of();
        private final List<ApprovalStep> updatedSteps = new ArrayList<>();

        @Override
        public boolean existsRunningByEntity(ApprovalEntityType entityType, UUID entityId) {
            return false;
        }

        @Override
        public void save(ApprovalProcess process) {
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
            return Optional.empty();
        }

        @Override
        public List<ApprovalProcess> findRunningProcessesWithOverdueSteps(Instant now, int limit) {
            return processes;
        }

        @Override
        public void updateProcessRuntime(ApprovalProcess process) {
        }

        @Override
        public void updateStep(ApprovalStep step) {
            updatedSteps.add(step);
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

    private static final class FakeOrgApproverPort implements OrgApproverPort {
        private List<ApproverCandidate> candidates = List.of(
                candidate(MANAGER_ID, "manager"),
                candidate(ESCALATION_TARGET_ID, "manager-alt"));

        @Override
        public List<ApproverCandidate> resolveApprovers(ResolveApproverQuery query) {
            return candidates;
        }
    }

    private static final class FakeApprovalSlaBreachedEventPublisher implements ApprovalSlaBreachedEventPublisher {
        private final List<ApprovalSlaBreachedEvent> events = new ArrayList<>();

        @Override
        public void publish(ApprovalSlaBreachedEvent event) {
            events.add(event);
        }
    }
}
