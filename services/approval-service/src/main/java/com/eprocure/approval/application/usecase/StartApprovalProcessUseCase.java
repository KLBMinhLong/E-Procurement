package com.eprocure.approval.application.usecase;

import com.eprocure.approval.application.port.in.ResolveApprovalChainCommand;
import com.eprocure.approval.application.port.in.StartApprovalProcessCommand;
import com.eprocure.approval.application.port.out.ApprovalStepAssignedEventPublisher;
import com.eprocure.approval.application.port.out.ApprovalWorkflowPort;
import com.eprocure.approval.application.port.out.ApprovalWorkflowPort.StartWorkflowCommand;
import com.eprocure.approval.application.port.out.PurchaseRequestStatusPort;
import com.eprocure.approval.application.port.out.PurchaseRequestStatusPort.MarkPendingApprovalCommand;
import com.eprocure.approval.application.service.ApprovalChainResolutionService;
import com.eprocure.approval.application.service.ResolvedApprovalChainView;
import com.eprocure.approval.application.service.StartApprovalProcessResult;
import com.eprocure.approval.application.service.StartedApprovalProcessView;
import com.eprocure.approval.application.service.StartedApprovalProcessView.StartedApprovalStepView;
import com.eprocure.approval.common.util.LogMaskingUtil;
import com.eprocure.approval.domain.event.ApprovalStepAssignedEvent;
import com.eprocure.approval.domain.event.ApprovalStepAssignedEvent.Payload;
import com.eprocure.approval.domain.model.ApprovalEntityType;
import com.eprocure.approval.domain.model.ApprovalProcess;
import com.eprocure.approval.domain.model.PurchaseRequestPriority;
import com.eprocure.approval.domain.repository.ApprovalProcessRepository;
import com.eprocure.approval.domain.repository.EventProcessingLogRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StartApprovalProcessUseCase {
    private static final Logger log = LogManager.getLogger(StartApprovalProcessUseCase.class);
    private static final String HANDLER_NAME = "StartApprovalProcessUseCase";
    private static final String STANDARD_PROCESS_KEY = "pr-approval-process";
    private static final String EMERGENCY_PROCESS_KEY = "emergency-approval";

    private final EventProcessingLogRepository eventProcessingLogRepository;
    private final ApprovalProcessRepository approvalProcessRepository;
    private final ApprovalChainResolutionService approvalChainResolutionService;
    private final ApprovalWorkflowPort approvalWorkflowPort;
    private final ApprovalStepAssignedEventPublisher stepAssignedEventPublisher;
    private final PurchaseRequestStatusPort purchaseRequestStatusPort;
    private final Clock clock;

    public StartApprovalProcessUseCase(
            EventProcessingLogRepository eventProcessingLogRepository,
            ApprovalProcessRepository approvalProcessRepository,
            ApprovalChainResolutionService approvalChainResolutionService,
            ApprovalWorkflowPort approvalWorkflowPort,
            ApprovalStepAssignedEventPublisher stepAssignedEventPublisher,
            PurchaseRequestStatusPort purchaseRequestStatusPort,
            Clock clock) {
        this.eventProcessingLogRepository = eventProcessingLogRepository;
        this.approvalProcessRepository = approvalProcessRepository;
        this.approvalChainResolutionService = approvalChainResolutionService;
        this.approvalWorkflowPort = approvalWorkflowPort;
        this.stepAssignedEventPublisher = stepAssignedEventPublisher;
        this.purchaseRequestStatusPort = purchaseRequestStatusPort;
        this.clock = clock;
    }

    @Transactional
    public StartApprovalProcessResult execute(StartApprovalProcessCommand command) {
        if (eventProcessingLogRepository.existsByEventId(command.eventId())) {
            log.info("[ACTION] Skip StartApprovalProcess duplicate event | eventId={}", command.eventId());
            return StartApprovalProcessResult.skippedEvent();
        }

        log.info("[ACTION] Start StartApprovalProcess | eventId={} | prId={} | prNumber={}",
                command.eventId(),
                LogMaskingUtil.maskId(command.purchaseRequestId()),
                command.prNumber());

        if (approvalProcessRepository.existsRunningByEntity(ApprovalEntityType.PURCHASE_REQUEST, command.purchaseRequestId())) {
            eventProcessingLogRepository.markSkipped(
                    command.eventId(),
                    command.topic(),
                    command.partitionId(),
                    command.offsetValue(),
                    HANDLER_NAME);
            log.info("[ACTION] Skip StartApprovalProcess running exists | eventId={} | prId={}",
                    command.eventId(),
                    LogMaskingUtil.maskId(command.purchaseRequestId()));
            return StartApprovalProcessResult.skippedEvent();
        }

        ResolvedApprovalChainView chain = approvalChainResolutionService.resolve(new ResolveApprovalChainCommand(
                command.purchaseRequestId(),
                command.requesterId(),
                command.departmentId(),
                command.totalAmount(),
                command.categories(),
                command.priority()));

        String processDefinitionKey = processDefinitionKey(command.priority());
        String firstApproverId = chain.steps().get(0).approver().id().toString();
        ApprovalWorkflowPort.StartedWorkflow workflow = approvalWorkflowPort.start(new StartWorkflowCommand(
                processDefinitionKey,
                command.purchaseRequestId().toString(),
                workflowVariables(command, chain, firstApproverId)));

        ApprovalProcess process = ApprovalProcess.createForPurchaseRequest(
                command.purchaseRequestId(),
                command.prNumber(),
                command.title(),
                command.requesterId(),
                command.departmentId(),
                command.totalAmount(),
                command.priority(),
                workflow.processInstanceId(),
                command.entitySnapshot(),
                chain,
                clock.instant());
        approvalProcessRepository.save(process);
        eventProcessingLogRepository.markProcessed(
                command.eventId(),
                command.topic(),
                command.partitionId(),
                command.offsetValue(),
                HANDLER_NAME);

        StartedApprovalProcessView view = toView(process, chain.primaryRuleName());
        purchaseRequestStatusPort.markPendingApproval(new MarkPendingApprovalCommand(
                command.purchaseRequestId(),
                process.getId(),
                process.getCamundaProcessInstanceId()));
        publishInitialStepAssignments(view, command.traceId());

        log.info("[ACTION] Complete StartApprovalProcess | eventId={} | processId={} | camundaProcessInstanceId={}",
                command.eventId(),
                LogMaskingUtil.maskId(process.getId()),
                workflow.processInstanceId());
        return StartApprovalProcessResult.started(view);
    }

    private String processDefinitionKey(PurchaseRequestPriority priority) {
        return priority == PurchaseRequestPriority.EMERGENCY ? EMERGENCY_PROCESS_KEY : STANDARD_PROCESS_KEY;
    }

    private Map<String, Object> workflowVariables(
            StartApprovalProcessCommand command,
            ResolvedApprovalChainView chain,
            String firstApproverId) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("purchaseRequestId", command.purchaseRequestId().toString());
        variables.put("prNumber", command.prNumber());
        variables.put("requesterId", command.requesterId().toString());
        variables.put("departmentId", command.departmentId().toString());
        variables.put("priority", command.priority().name());
        variables.put("primaryRuleName", chain.primaryRuleName());
        variables.put("approverId", firstApproverId);
        variables.put("approvalStepCount", chain.steps().size());
        chain.steps().forEach(step -> approverVariableNames(step.requiredPermission())
                .forEach(variableName -> variables.putIfAbsent(variableName, step.approver().id().toString())));
        return variables;
    }

    private java.util.List<String> approverVariableNames(String requiredPermission) {
        String permission = requiredPermission.trim().toUpperCase(Locale.ROOT);
        return switch (permission) {
            case "PR_APPROVE_L1" -> java.util.List.of("managerApproverId", permissionVariableName(permission));
            case "PR_APPROVE_L2" -> java.util.List.of("directorApproverId", permissionVariableName(permission));
            case "PR_APPROVE_FINANCE" -> java.util.List.of("financeApproverId", "postAuditApproverId", permissionVariableName(permission));
            case "PR_APPROVE_EMERGENCY" -> java.util.List.of("managerApproverId", permissionVariableName(permission));
            default -> java.util.List.of(permissionVariableName(permission));
        };
    }

    private String permissionVariableName(String requiredPermission) {
        String[] parts = requiredPermission.trim().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder(parts[0]);
        for (int index = 1; index < parts.length; index++) {
            if (!parts[index].isBlank()) {
                builder.append(Character.toUpperCase(parts[index].charAt(0)))
                        .append(parts[index].substring(1));
            }
        }
        return builder.append("ApproverId").toString();
    }

    private void publishInitialStepAssignments(StartedApprovalProcessView process, UUID traceId) {
        process.steps().stream()
                .filter(step -> step.sequence() == process.currentStepIndex())
                .map(step -> ApprovalStepAssignedEvent.create(
                        traceId,
                        clock.instant(),
                        new Payload(
                                process.processId(),
                                step.stepId(),
                                process.purchaseRequestId(),
                                process.prNumber(),
                                process.priority(),
                                step.sequence(),
                                step.stepType(),
                                step.requiredPermission(),
                                step.approverId(),
                                step.assignedAt(),
                                step.slaDeadline())))
                .forEach(stepAssignedEventPublisher::publish);
    }

    private StartedApprovalProcessView toView(ApprovalProcess process, String primaryRuleName) {
        return new StartedApprovalProcessView(
                process.getId(),
                process.getEntityId(),
                process.getEntityNumber(),
                process.getPriority(),
                process.getStatus(),
                process.getCurrentStepIndex(),
                process.getCamundaProcessInstanceId(),
                primaryRuleName,
                process.getStartedAt(),
                process.getSteps().stream()
                        .map(step -> new StartedApprovalStepView(
                                step.getId(),
                                step.getStepIndex(),
                                step.getStepType(),
                                step.getRequiredPermission(),
                                step.getApproverId(),
                                step.getStatus(),
                                step.getAssignedAt(),
                                step.getSlaDeadline()))
                        .toList());
    }
}
