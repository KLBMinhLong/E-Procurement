package com.eprocure.approval.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.approval.application.port.in.StartApprovalProcessCommand;
import com.eprocure.approval.application.port.out.ApprovalStepAssignedEventPublisher;
import com.eprocure.approval.application.port.out.ApprovalWorkflowPort;
import com.eprocure.approval.application.port.out.OrgApproverPort;
import com.eprocure.approval.application.port.out.PurchaseRequestStatusPort;
import com.eprocure.approval.application.service.ApprovalChainResolutionService;
import com.eprocure.approval.application.service.ApprovalRuleSelectionService;
import com.eprocure.approval.application.service.BusinessHoursCalendar;
import com.eprocure.approval.application.service.SlaDeadlineCalculator;
import com.eprocure.approval.application.service.StartApprovalProcessResult;
import com.eprocure.approval.domain.event.ApprovalStepAssignedEvent;
import com.eprocure.approval.domain.model.ApprovalCondition;
import com.eprocure.approval.domain.model.ApprovalEntityType;
import com.eprocure.approval.domain.model.ApprovalProcess;
import com.eprocure.approval.domain.model.ApprovalProcessStatus;
import com.eprocure.approval.domain.model.ApprovalRule;
import com.eprocure.approval.domain.model.ApprovalRuleType;
import com.eprocure.approval.domain.model.ApprovalStep;
import com.eprocure.approval.domain.model.ApprovalStepStatus;
import com.eprocure.approval.domain.model.ApprovalStepTemplate;
import com.eprocure.approval.domain.model.ApprovalStepType;
import com.eprocure.approval.domain.model.PurchaseRequestPriority;
import com.eprocure.approval.domain.model.vo.Money;
import com.eprocure.approval.domain.repository.ApprovalProcessRepository;
import com.eprocure.approval.domain.repository.ApprovalRuleRepository;
import com.eprocure.approval.domain.repository.EventProcessingLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StartApprovalProcessUseCaseTest {
    private static final UUID PR_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440100");
    private static final UUID DEPARTMENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID REQUESTER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID MANAGER_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID FINANCE_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID DIRECTOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000004");
    private static final UUID POST_AUDIT_ID = UUID.fromString("30000000-0000-0000-0000-000000000005");
    private static final UUID TRACE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant MONDAY_08_VN = Instant.parse("2026-06-01T01:00:00Z");

    @Test
    void should_start_process_when_event_is_new() {
        FakeEventProcessingLogRepository eventLogRepository = new FakeEventProcessingLogRepository(false);
        FakeApprovalProcessRepository processRepository = new FakeApprovalProcessRepository(false);
        FakeApprovalWorkflowPort workflowPort = new FakeApprovalWorkflowPort("camunda-001");
        FakeApprovalStepAssignedEventPublisher eventPublisher = new FakeApprovalStepAssignedEventPublisher();
        FakePurchaseRequestStatusPort purchaseRequestStatusPort = new FakePurchaseRequestStatusPort();
        StartApprovalProcessUseCase useCase = newUseCase(
                eventLogRepository,
                processRepository,
                workflowPort,
                eventPublisher,
                purchaseRequestStatusPort);

        StartApprovalProcessResult result = useCase.execute(defaultCommand(PurchaseRequestPriority.NORMAL));

        assertThat(result.skipped()).isFalse();
        assertThat(result.process()).isPresent();
        assertThat(result.process().orElseThrow().status()).isEqualTo(ApprovalProcessStatus.RUNNING);
        assertThat(result.process().orElseThrow().camundaProcessInstanceId()).isEqualTo("camunda-001");
        assertThat(result.process().orElseThrow().startedAt()).isEqualTo(MONDAY_08_VN);

        assertThat(processRepository.savedProcess).isNotNull();
        assertThat(processRepository.savedProcess.getEntityId()).isEqualTo(PR_ID);
        assertThat(processRepository.savedProcess.getSteps()).hasSize(2);
        assertThat(processRepository.savedProcess.getSteps())
                .extracting(step -> step.getApproverId())
                .containsExactly(MANAGER_ID, FINANCE_ID);
        assertThat(processRepository.savedProcess.getSteps())
                .extracting(step -> step.getStatus())
                .containsOnly(ApprovalStepStatus.PENDING);
        assertThat(processRepository.savedProcess.getSteps())
                .extracting(step -> step.getAssignedAt())
                .containsOnly(MONDAY_08_VN);

        assertThat(workflowPort.startedCommands).hasSize(1);
        assertThat(workflowPort.startedCommands.get(0).processDefinitionKey()).isEqualTo("pr-approval-process");
        assertThat(workflowPort.startedCommands.get(0).businessKey()).isEqualTo(PR_ID.toString());
        assertThat(workflowPort.startedCommands.get(0).variables())
                .containsEntry("approverId", MANAGER_ID.toString())
                .containsEntry("managerApproverId", MANAGER_ID.toString())
                .containsEntry("financeApproverId", FINANCE_ID.toString())
                .containsEntry("approvalStepCount", 2)
                .containsEntry("priority", "NORMAL");
        assertThat(eventLogRepository.operations).containsExactly("exists", "processed");
        assertThat(eventPublisher.events).hasSize(1);
        assertThat(eventPublisher.events.get(0).traceId()).isEqualTo(TRACE_ID);
        assertThat(eventPublisher.events.get(0).payload().approverId()).isEqualTo(MANAGER_ID);
        assertThat(eventPublisher.events.get(0).payload().stepIndex()).isEqualTo(1);
        assertThat(eventPublisher.events.get(0).payload().priority()).isEqualTo(PurchaseRequestPriority.NORMAL);
        assertThat(purchaseRequestStatusPort.commands).hasSize(1);
        assertThat(purchaseRequestStatusPort.commands.get(0).purchaseRequestId()).isEqualTo(PR_ID);
        assertThat(purchaseRequestStatusPort.commands.get(0).approvalProcessId()).isEqualTo(processRepository.savedProcess.getId());
        assertThat(purchaseRequestStatusPort.commands.get(0).camundaProcessInstanceId()).isEqualTo("camunda-001");
    }

    @Test
    void should_skip_when_event_was_already_processed() {
        FakeEventProcessingLogRepository eventLogRepository = new FakeEventProcessingLogRepository(true);
        FakeApprovalProcessRepository processRepository = new FakeApprovalProcessRepository(false);
        FakeApprovalWorkflowPort workflowPort = new FakeApprovalWorkflowPort("camunda-001");
        FakeApprovalStepAssignedEventPublisher eventPublisher = new FakeApprovalStepAssignedEventPublisher();
        FakePurchaseRequestStatusPort purchaseRequestStatusPort = new FakePurchaseRequestStatusPort();
        StartApprovalProcessUseCase useCase = newUseCase(
                eventLogRepository,
                processRepository,
                workflowPort,
                eventPublisher,
                purchaseRequestStatusPort);

        StartApprovalProcessResult result = useCase.execute(defaultCommand(PurchaseRequestPriority.NORMAL));

        assertThat(result.skipped()).isTrue();
        assertThat(result.process()).isEmpty();
        assertThat(processRepository.savedProcess).isNull();
        assertThat(workflowPort.startedCommands).isEmpty();
        assertThat(eventPublisher.events).isEmpty();
        assertThat(purchaseRequestStatusPort.commands).isEmpty();
        assertThat(eventLogRepository.operations).containsExactly("exists");
    }

    @Test
    void should_mark_skipped_when_running_process_exists() {
        FakeEventProcessingLogRepository eventLogRepository = new FakeEventProcessingLogRepository(false);
        FakeApprovalProcessRepository processRepository = new FakeApprovalProcessRepository(true);
        FakeApprovalWorkflowPort workflowPort = new FakeApprovalWorkflowPort("camunda-001");
        FakeApprovalStepAssignedEventPublisher eventPublisher = new FakeApprovalStepAssignedEventPublisher();
        FakePurchaseRequestStatusPort purchaseRequestStatusPort = new FakePurchaseRequestStatusPort();
        StartApprovalProcessUseCase useCase = newUseCase(
                eventLogRepository,
                processRepository,
                workflowPort,
                eventPublisher,
                purchaseRequestStatusPort);

        StartApprovalProcessResult result = useCase.execute(defaultCommand(PurchaseRequestPriority.NORMAL));

        assertThat(result.skipped()).isTrue();
        assertThat(result.process()).isEmpty();
        assertThat(processRepository.savedProcess).isNull();
        assertThat(workflowPort.startedCommands).isEmpty();
        assertThat(eventPublisher.events).isEmpty();
        assertThat(purchaseRequestStatusPort.commands).isEmpty();
        assertThat(eventLogRepository.operations).containsExactly("exists", "skipped");
        assertThat(eventLogRepository.lastEventId).isEqualTo("evt-pr-submitted-001");
    }

    @Test
    void should_start_emergency_bpmn_when_priority_is_emergency() {
        FakeEventProcessingLogRepository eventLogRepository = new FakeEventProcessingLogRepository(false);
        FakeApprovalProcessRepository processRepository = new FakeApprovalProcessRepository(false);
        FakeApprovalWorkflowPort workflowPort = new FakeApprovalWorkflowPort("camunda-emergency-001");
        FakeApprovalStepAssignedEventPublisher eventPublisher = new FakeApprovalStepAssignedEventPublisher();
        FakePurchaseRequestStatusPort purchaseRequestStatusPort = new FakePurchaseRequestStatusPort();
        StartApprovalProcessUseCase useCase = newUseCase(
                eventLogRepository,
                processRepository,
                workflowPort,
                eventPublisher,
                purchaseRequestStatusPort);

        StartApprovalProcessResult result = useCase.execute(defaultCommand(PurchaseRequestPriority.EMERGENCY));

        assertThat(result.skipped()).isFalse();
        assertThat(workflowPort.startedCommands).hasSize(1);
        assertThat(workflowPort.startedCommands.get(0).processDefinitionKey()).isEqualTo("emergency-approval");
        assertThat(workflowPort.startedCommands.get(0).variables())
                .containsEntry("managerApproverId", MANAGER_ID.toString())
                .containsEntry("directorApproverId", DIRECTOR_ID.toString())
                .containsEntry("postAuditApproverId", POST_AUDIT_ID.toString())
                .containsEntry("approvalStepCount", 3);
        assertThat(processRepository.savedProcess.getPriority()).isEqualTo(PurchaseRequestPriority.EMERGENCY);
        assertThat(processRepository.savedProcess.getSteps())
                .extracting(step -> step.getApproverId())
                .containsExactly(MANAGER_ID, DIRECTOR_ID, POST_AUDIT_ID);
        assertThat(eventPublisher.events)
                .extracting(event -> event.payload().approverId())
                .containsExactly(MANAGER_ID, DIRECTOR_ID);
        assertThat(purchaseRequestStatusPort.commands).hasSize(1);
    }

    private StartApprovalProcessUseCase newUseCase(
            FakeEventProcessingLogRepository eventLogRepository,
            FakeApprovalProcessRepository processRepository,
            FakeApprovalWorkflowPort workflowPort,
            FakeApprovalStepAssignedEventPublisher eventPublisher,
            FakePurchaseRequestStatusPort purchaseRequestStatusPort) {
        FakeOrgApproverPort orgApproverPort = new FakeOrgApproverPort(Map.of(
                "MANAGER", List.of(candidate(MANAGER_ID, "manager")),
                "FINANCE", List.of(candidate(FINANCE_ID, "finance")),
                "DIRECTOR", List.of(candidate(DIRECTOR_ID, "director")),
                "POST_AUDIT", List.of(candidate(POST_AUDIT_ID, "post-audit"))));
        ApprovalChainResolutionService approvalChainResolutionService = new ApprovalChainResolutionService(
                new ApprovalRuleSelectionService(new StubApprovalRuleRepository(List.of(emergencyRule(), defaultRule()))),
                orgApproverPort,
                new SlaDeadlineCalculator(
                        BusinessHoursCalendar.from("Asia/Ho_Chi_Minh", "08:00", "17:30", "MON,TUE,WED,THU,FRI"),
                        Clock.fixed(MONDAY_08_VN, ZoneOffset.UTC)));
        return new StartApprovalProcessUseCase(
                eventLogRepository,
                processRepository,
                approvalChainResolutionService,
                workflowPort,
                eventPublisher,
                purchaseRequestStatusPort,
                Clock.fixed(MONDAY_08_VN, ZoneOffset.UTC));
    }

    private StartApprovalProcessCommand defaultCommand(PurchaseRequestPriority priority) {
        return new StartApprovalProcessCommand(
                "evt-pr-submitted-001",
                "procurement.pr.submitted",
                1,
                25L,
                TRACE_ID,
                PR_ID,
                "PR-2026-05-00001",
                "Mua thiet bi van phong",
                REQUESTER_ID,
                DEPARTMENT_ID,
                Money.vnd("12000000.0000"),
                Set.of("OFFICE_SUPPLIES"),
                priority,
                Map.of(
                        "purchaseRequestId", PR_ID.toString(),
                        "prNumber", "PR-2026-05-00001"));
    }

    private ApprovalRule defaultRule() {
        return ApprovalRule.restore(
                UUID.fromString("550e8400-e29b-41d4-a716-446655441000"),
                "VALUE_DEFAULT",
                100,
                true,
                ApprovalRuleType.DEFAULT,
                ApprovalCondition.of(null, null, Set.of(), Set.of(), Set.of(PurchaseRequestPriority.NORMAL)),
                List.of(
                        new ApprovalStepTemplate(1, "MANAGER", ApprovalStepType.SEQUENTIAL, 24, true),
                        new ApprovalStepTemplate(2, "FINANCE", ApprovalStepType.SEQUENTIAL, 48, true)),
                null);
    }

    private ApprovalRule emergencyRule() {
        return ApprovalRule.restore(
                UUID.fromString("550e8400-e29b-41d4-a716-446655441001"),
                "EMERGENCY_DEFAULT",
                1000,
                true,
                ApprovalRuleType.DEFAULT,
                ApprovalCondition.of(null, null, Set.of(), Set.of(), Set.of(PurchaseRequestPriority.EMERGENCY)),
                List.of(
                        new ApprovalStepTemplate(1, "MANAGER", ApprovalStepType.PARALLEL, 2, true),
                        new ApprovalStepTemplate(1, "DIRECTOR", ApprovalStepType.PARALLEL, 4, true),
                        new ApprovalStepTemplate(2, "POST_AUDIT", ApprovalStepType.SEQUENTIAL, 24, true)),
                null);
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

    private record StubApprovalRuleRepository(List<ApprovalRule> rules) implements ApprovalRuleRepository {
        @Override
        public List<ApprovalRule> findActiveRules() {
            return rules;
        }
    }

    private static final class FakeOrgApproverPort implements OrgApproverPort {
        private final Map<String, List<ApproverCandidate>> candidatesByRole;

        private FakeOrgApproverPort(Map<String, List<ApproverCandidate>> candidatesByRole) {
            this.candidatesByRole = candidatesByRole;
        }

        @Override
        public List<ApproverCandidate> resolveApprovers(ResolveApproverQuery query) {
            return candidatesByRole.getOrDefault(query.approverRole(), List.of());
        }
    }

    private static final class FakeEventProcessingLogRepository implements EventProcessingLogRepository {
        private final boolean processed;
        private final List<String> operations = new ArrayList<>();
        private String lastEventId;

        private FakeEventProcessingLogRepository(boolean processed) {
            this.processed = processed;
        }

        @Override
        public boolean existsByEventId(String eventId) {
            operations.add("exists");
            lastEventId = eventId;
            return processed;
        }

        @Override
        public void markProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName) {
            operations.add("processed");
            lastEventId = eventId;
        }

        @Override
        public void markSkipped(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName) {
            operations.add("skipped");
            lastEventId = eventId;
        }
    }

    private static final class FakeApprovalProcessRepository implements ApprovalProcessRepository {
        private final boolean runningExists;
        private ApprovalProcess savedProcess;

        private FakeApprovalProcessRepository(boolean runningExists) {
            this.runningExists = runningExists;
        }

        @Override
        public boolean existsRunningByEntity(ApprovalEntityType entityType, UUID entityId) {
            assertThat(entityType).isEqualTo(ApprovalEntityType.PURCHASE_REQUEST);
            assertThat(entityId).isEqualTo(PR_ID);
            return runningExists;
        }

        @Override
        public void save(ApprovalProcess process) {
            savedProcess = process;
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

    private static final class FakeApprovalWorkflowPort implements ApprovalWorkflowPort {
        private final String processInstanceId;
        private final List<StartWorkflowCommand> startedCommands = new ArrayList<>();

        private FakeApprovalWorkflowPort(String processInstanceId) {
            this.processInstanceId = processInstanceId;
        }

        @Override
        public StartedWorkflow start(StartWorkflowCommand command) {
            startedCommands.add(command);
            return new StartedWorkflow(processInstanceId);
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
        private final List<MarkPendingApprovalCommand> commands = new ArrayList<>();

        @Override
        public void markPendingApproval(MarkPendingApprovalCommand command) {
            commands.add(command);
        }

        @Override
        public void markApproved(ApprovalResultCommand command) {
        }

        @Override
        public void markRejected(ApprovalResultCommand command) {
        }

        @Override
        public void markChangesRequested(ApprovalResultCommand command) {
        }
    }
}
