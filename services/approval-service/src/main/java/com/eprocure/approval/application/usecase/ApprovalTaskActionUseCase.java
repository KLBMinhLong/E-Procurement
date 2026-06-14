package com.eprocure.approval.application.usecase;

import com.eprocure.approval.application.port.in.ApprovalTaskActionCommand;
import com.eprocure.approval.application.port.out.ApprovalStepAssignedEventPublisher;
import com.eprocure.approval.application.port.out.OrgApproverPort;
import com.eprocure.approval.application.port.out.OrgApproverPort.ResolveApproverQuery;
import com.eprocure.approval.application.port.out.PurchaseRequestStatusPort;
import com.eprocure.approval.application.port.out.PurchaseRequestStatusPort.ApprovalResultCommand;
import com.eprocure.approval.application.service.ApprovalTaskActionResult;
import com.eprocure.approval.application.service.ApprovalTaskActionView;
import com.eprocure.approval.application.service.IdempotencyService;
import com.eprocure.approval.common.exception.BusinessException;
import com.eprocure.approval.common.exception.ErrorCode;
import com.eprocure.approval.common.util.LogMaskingUtil;
import com.eprocure.approval.domain.event.ApprovalStepAssignedEvent;
import com.eprocure.approval.domain.event.ApprovalStepAssignedEvent.Payload;
import com.eprocure.approval.domain.model.ApprovalAction;
import com.eprocure.approval.domain.model.ApprovalEntityType;
import com.eprocure.approval.domain.model.ApprovalProcess;
import com.eprocure.approval.domain.model.ApprovalProcess.ApprovalActionResult;
import com.eprocure.approval.domain.model.ApprovalStep;
import com.eprocure.approval.domain.repository.ApprovalProcessRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalTaskActionUseCase {
    private static final Logger log = LogManager.getLogger(ApprovalTaskActionUseCase.class);
    private static final String IDEMPOTENCY_OPERATION = "approval-task-action";
    private static final int REQUIRED_COMMENT_MIN_LENGTH = 20;
    private static final int FORWARD_REASON_MIN_LENGTH = 10;

    private final ApprovalProcessRepository approvalProcessRepository;
    private final IdempotencyService idempotencyService;
    private final ApprovalStepAssignedEventPublisher stepAssignedEventPublisher;
    private final PurchaseRequestStatusPort purchaseRequestStatusPort;
    private final OrgApproverPort orgApproverPort;
    private final Clock clock;

    public ApprovalTaskActionUseCase(
            ApprovalProcessRepository approvalProcessRepository,
            IdempotencyService idempotencyService,
            ApprovalStepAssignedEventPublisher stepAssignedEventPublisher,
            PurchaseRequestStatusPort purchaseRequestStatusPort,
            OrgApproverPort orgApproverPort,
            Clock clock) {
        this.approvalProcessRepository = approvalProcessRepository;
        this.idempotencyService = idempotencyService;
        this.stepAssignedEventPublisher = stepAssignedEventPublisher;
        this.purchaseRequestStatusPort = purchaseRequestStatusPort;
        this.orgApproverPort = orgApproverPort;
        this.clock = clock;
    }

    @Transactional
    public ApprovalTaskActionResult execute(ApprovalTaskActionCommand command, String idempotencyKey) {
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(
                IDEMPOTENCY_OPERATION,
                command.actorId(),
                idempotencyKey,
                ApprovalTaskActionView.class);
        if (cached.isPresent()) {
            log.info("[ACTION] Idempotency hit ApprovalTaskAction | taskId={} | actorId={} | key={}",
                    command.taskId(),
                    LogMaskingUtil.maskId(command.actorId()),
                    LogMaskingUtil.maskToken(idempotencyKey));
            return ApprovalTaskActionResult.replayed(cached.get());
        }

        ApprovalProcess process = findRunningProcess(command.taskId())
                .orElseThrow(() -> new BusinessException(ErrorCode.APR_004));
        UUID stepId = resolveStepId(process, command.taskId());
        validateCommand(command, process, stepId);
        Set<UUID> existingStepIds = process.getSteps().stream()
                .map(ApprovalStep::getId)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));

        log.info("[ACTION] Start ApprovalTaskAction | action={} | taskId={} | actorId={}",
                command.action(),
                stepId,
                LogMaskingUtil.maskId(command.actorId()));
        Instant actedAt = Instant.now(clock);
        ApprovalActionResult actionResult = applyAction(process, stepId, command, actedAt);
        persist(process, existingStepIds);
        applyPurchaseRequestStatus(process, command, actionResult.completed(), idempotencyKey);
        actionResult.assignedSteps().forEach(step -> publishStepAssigned(process, step));

        ApprovalTaskActionView view = toView(process, stepId, command.action(), actionResult);
        idempotencyService.save(IDEMPOTENCY_OPERATION, command.actorId(), idempotencyKey, view);
        log.info("[ACTION] Complete ApprovalTaskAction | action={} | taskId={} | processId={} | completed={}",
                command.action(),
                stepId,
                LogMaskingUtil.maskId(process.getId()),
                actionResult.completed());
        return ApprovalTaskActionResult.fresh(view);
    }

    private Optional<ApprovalProcess> findRunningProcess(String taskId) {
        try {
            return approvalProcessRepository.findRunningByStepId(UUID.fromString(taskId));
        } catch (IllegalArgumentException ignored) {
            return approvalProcessRepository.findRunningByCamundaTaskId(taskId);
        }
    }

    private UUID resolveStepId(ApprovalProcess process, String taskId) {
        try {
            return UUID.fromString(taskId);
        } catch (IllegalArgumentException ignored) {
            return process.getSteps().stream()
                    .filter(step -> step.getCamundaTaskId().filter(taskId::equals).isPresent())
                    .map(ApprovalStep::getId)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.APR_004));
        }
    }

    private void validateCommand(ApprovalTaskActionCommand command, ApprovalProcess process, UUID stepId) {
        if ((command.action() == ApprovalAction.REJECT || command.action() == ApprovalAction.REQUEST_CHANGES)
                && (command.comment() == null || command.comment().length() < REQUIRED_COMMENT_MIN_LENGTH)) {
            throw new BusinessException(ErrorCode.APR_005);
        }
        if (command.action() == ApprovalAction.FORWARD) {
            if (command.forwardToUserId() == null
                    || command.comment() == null
                    || command.comment().length() < FORWARD_REASON_MIN_LENGTH) {
                throw new BusinessException(ErrorCode.APR_006);
            }
            ApprovalStep step = process.findStep(stepId).orElseThrow(() -> new BusinessException(ErrorCode.APR_004));
            boolean eligible = orgApproverPort.resolveApprovers(new ResolveApproverQuery(
                            step.getRequiredPermission(),
                            process.getRequesterDepartmentId(),
                            process.getRequesterId()))
                    .stream()
                    .anyMatch(candidate -> candidate.id().equals(command.forwardToUserId()));
            if (!eligible) {
                throw new BusinessException(ErrorCode.APR_006);
            }
        }
    }

    private ApprovalActionResult applyAction(
            ApprovalProcess process,
            UUID stepId,
            ApprovalTaskActionCommand command,
            Instant actedAt) {
        try {
            return switch (command.action()) {
                case APPROVE -> process.approve(stepId, command.actorId(), command.comment(), actedAt);
                case REJECT -> process.reject(stepId, command.actorId(), command.comment(), actedAt);
                case REQUEST_CHANGES -> process.requestChanges(stepId, command.actorId(), command.comment(), actedAt);
                case FORWARD -> process.forward(stepId, command.actorId(), command.forwardToUserId(), command.comment(), actedAt);
            };
        } catch (SecurityException exception) {
            throw new BusinessException(ErrorCode.IAM_004);
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.APR_003);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(command.action() == ApprovalAction.FORWARD ? ErrorCode.APR_006 : ErrorCode.APR_004);
        }
    }

    private void persist(ApprovalProcess process, Set<UUID> existingStepIds) {
        approvalProcessRepository.updateProcessRuntime(process);
        for (ApprovalStep step : process.getSteps()) {
            if (existingStepIds.contains(step.getId())) {
                approvalProcessRepository.updateStep(step);
            } else {
                approvalProcessRepository.insertStep(step);
            }
        }
    }

    private void applyPurchaseRequestStatus(
            ApprovalProcess process,
            ApprovalTaskActionCommand command,
            boolean completed,
            String idempotencyKey) {
        if (process.getEntityType() != ApprovalEntityType.PURCHASE_REQUEST) {
            return;
        }
        ApprovalResultCommand resultCommand = new ApprovalResultCommand(
                process.getEntityId(),
                process.getId(),
                idempotencyKey,
                command.comment());
        if (command.action() == ApprovalAction.REJECT) {
            purchaseRequestStatusPort.markRejected(resultCommand);
        } else if (command.action() == ApprovalAction.REQUEST_CHANGES) {
            purchaseRequestStatusPort.markChangesRequested(resultCommand);
        } else if (command.action() == ApprovalAction.APPROVE && completed) {
            purchaseRequestStatusPort.markApproved(resultCommand);
        }
    }

    private void publishStepAssigned(ApprovalProcess process, ApprovalStep step) {
        stepAssignedEventPublisher.publish(ApprovalStepAssignedEvent.create(
                UUID.randomUUID(),
                Instant.now(clock),
                new Payload(
                        process.getId(),
                        step.getId(),
                        process.getEntityId(),
                        process.getEntityNumber(),
                        process.getPriority(),
                        step.getStepIndex(),
                        step.getStepType(),
                        step.getRequiredPermission(),
                        step.getApproverId(),
                        step.getAssignedAt(),
                        step.getSlaDeadline())));
    }

    private ApprovalTaskActionView toView(
            ApprovalProcess process,
            UUID stepId,
            ApprovalAction action,
            ApprovalActionResult actionResult) {
        return new ApprovalTaskActionView(
                process.getId(),
                stepId,
                action,
                process.getCurrentStepIndex(),
                actionResult.completed(),
                actionResult.assignedSteps().stream()
                        .map(step -> new ApprovalTaskActionView.AssignedStepView(
                                step.getId(),
                                step.getStepIndex(),
                                step.getRequiredPermission(),
                                step.getApproverId(),
                                step.getSlaDeadline()))
                        .toList());
    }
}
