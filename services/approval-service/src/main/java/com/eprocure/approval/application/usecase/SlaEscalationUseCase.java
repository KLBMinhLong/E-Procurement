package com.eprocure.approval.application.usecase;

import com.eprocure.approval.application.port.out.ApprovalSlaBreachedEventPublisher;
import com.eprocure.approval.application.port.out.OrgApproverPort;
import com.eprocure.approval.application.port.out.OrgApproverPort.ResolveApproverQuery;
import com.eprocure.approval.application.service.SlaEscalationResult;
import com.eprocure.approval.common.exception.BusinessException;
import com.eprocure.approval.common.util.LogMaskingUtil;
import com.eprocure.approval.domain.event.ApprovalSlaBreachedEvent;
import com.eprocure.approval.domain.event.ApprovalSlaBreachedEvent.Payload;
import com.eprocure.approval.domain.model.ApprovalProcess;
import com.eprocure.approval.domain.model.ApprovalStep;
import com.eprocure.approval.domain.model.ApprovalStepStatus;
import com.eprocure.approval.domain.repository.ApprovalProcessRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlaEscalationUseCase {
    private static final Logger log = LogManager.getLogger(SlaEscalationUseCase.class);

    private final ApprovalProcessRepository approvalProcessRepository;
    private final OrgApproverPort orgApproverPort;
    private final ApprovalSlaBreachedEventPublisher slaBreachedEventPublisher;
    private final Clock clock;
    private final int batchSize;

    public SlaEscalationUseCase(
            ApprovalProcessRepository approvalProcessRepository,
            OrgApproverPort orgApproverPort,
            ApprovalSlaBreachedEventPublisher slaBreachedEventPublisher,
            Clock clock,
            @Value("${eprocure.approval.sla.escalation-batch-size:50}") int batchSize) {
        this.approvalProcessRepository = approvalProcessRepository;
        this.orgApproverPort = orgApproverPort;
        this.slaBreachedEventPublisher = slaBreachedEventPublisher;
        this.clock = clock;
        this.batchSize = Math.max(1, batchSize);
    }

    @Transactional
    public SlaEscalationResult execute() {
        Instant now = Instant.now(clock);
        List<ApprovalProcess> processes = approvalProcessRepository.findRunningProcessesWithOverdueSteps(now, batchSize);
        List<SlaEscalationResult.EscalatedStepView> escalatedSteps = new ArrayList<>();

        log.info("[ACTION] Start SlaEscalation | now={} | scannedProcesses={}", now, processes.size());
        for (ApprovalProcess process : processes) {
            for (ApprovalStep step : overdueSteps(process, now)) {
                UUID previousApproverId = step.getApproverId();
                UUID escalationTargetId = resolveEscalationTarget(process, step)
                        .orElse(previousApproverId);

                step.escalateTo(escalationTargetId, now);
                approvalProcessRepository.updateStep(step);
                publishSlaBreached(process, step, previousApproverId, escalationTargetId, now);
                escalatedSteps.add(new SlaEscalationResult.EscalatedStepView(
                        process.getId(),
                        step.getId(),
                        previousApproverId,
                        escalationTargetId,
                        step.getSlaDeadline()));

                log.info("[ACTION] Step SlaEscalation escalated | processId={} | stepId={} | fromApproverId={} | toApproverId={}",
                        LogMaskingUtil.maskId(process.getId()),
                        LogMaskingUtil.maskId(step.getId()),
                        LogMaskingUtil.maskId(previousApproverId),
                        LogMaskingUtil.maskId(escalationTargetId));
            }
        }

        log.info("[ACTION] Complete SlaEscalation | scannedProcesses={} | escalatedSteps={}",
                processes.size(),
                escalatedSteps.size());
        return new SlaEscalationResult(processes.size(), escalatedSteps.size(), escalatedSteps);
    }

    private List<ApprovalStep> overdueSteps(ApprovalProcess process, Instant now) {
        return process.getSteps().stream()
                .filter(step -> step.getStatus() == ApprovalStepStatus.PENDING)
                .filter(step -> !step.isEscalated())
                .filter(step -> !step.getSlaDeadline().isAfter(now))
                .toList();
    }

    private Optional<UUID> resolveEscalationTarget(ApprovalProcess process, ApprovalStep step) {
        try {
            return orgApproverPort.resolveApprovers(new ResolveApproverQuery(
                            step.getApproverRole(),
                            process.getRequesterDepartmentId(),
                            process.getRequesterId()))
                    .stream()
                    .map(OrgApproverPort.ApproverCandidate::id)
                    .filter(candidateId -> !candidateId.equals(step.getApproverId()))
                    .filter(candidateId -> !candidateId.equals(process.getRequesterId()))
                    .findFirst();
        } catch (BusinessException exception) {
            log.warn("[ACTION] Step SlaEscalation resolve target failed | processId={} | stepId={} | role={} | errorCode={}",
                    LogMaskingUtil.maskId(process.getId()),
                    LogMaskingUtil.maskId(step.getId()),
                    step.getApproverRole(),
                    exception.getErrorCode().code());
            return Optional.empty();
        }
    }

    private void publishSlaBreached(
            ApprovalProcess process,
            ApprovalStep step,
            UUID breachedApproverId,
            UUID escalatedToApproverId,
            Instant breachedAt) {
        slaBreachedEventPublisher.publish(ApprovalSlaBreachedEvent.create(
                UUID.randomUUID(),
                breachedAt,
                new Payload(
                        process.getId(),
                        step.getId(),
                        process.getEntityId(),
                        process.getEntityNumber(),
                        process.getPriority(),
                        step.getStepIndex(),
                        step.getStepType(),
                        step.getApproverRole(),
                        breachedApproverId,
                        escalatedToApproverId,
                        !breachedApproverId.equals(escalatedToApproverId),
                        step.getAssignedAt(),
                        step.getSlaDeadline(),
                        breachedAt)));
    }
}
