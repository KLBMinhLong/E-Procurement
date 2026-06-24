package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.port.in.RestartServiceCommand;
import com.eprocure.admin.application.port.out.AdminAuditLogWriterPort;
import com.eprocure.admin.application.port.out.IamSecurityConfirmationPort;
import com.eprocure.admin.application.service.IdempotencyGuard;
import com.eprocure.admin.application.service.ServiceRestartResult;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.domain.model.AdminConfigAction;
import com.eprocure.admin.domain.model.AdminConfigActionType;
import com.eprocure.admin.domain.repository.AdminConfigActionRepository;
import com.eprocure.admin.domain.repository.ServiceConfigRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestartServiceUseCase {
    private static final Logger log = LogManager.getLogger(RestartServiceUseCase.class);
    private static final int MIN_REASON_LENGTH = 10;
    private static final int ESTIMATED_DOWNTIME_SECONDS = 30;

    private final ServiceConfigRepository serviceConfigRepository;
    private final AdminConfigActionRepository actionRepository;
    private final AdminAuditLogWriterPort auditLogWriter;
    private final IamSecurityConfirmationPort confirmationPort;
    private final IdempotencyGuard idempotencyGuard;
    private final Clock clock;

    public RestartServiceUseCase(
            ServiceConfigRepository serviceConfigRepository,
            AdminConfigActionRepository actionRepository,
            AdminAuditLogWriterPort auditLogWriter,
            IamSecurityConfirmationPort confirmationPort,
            IdempotencyGuard idempotencyGuard,
            Clock clock) {
        this.serviceConfigRepository = serviceConfigRepository;
        this.actionRepository = actionRepository;
        this.auditLogWriter = auditLogWriter;
        this.confirmationPort = confirmationPort;
        this.idempotencyGuard = idempotencyGuard;
        this.clock = clock;
    }

    @Transactional
    public ServiceRestartResult execute(RestartServiceCommand command, String idempotencyKey) {
        UUID key = idempotencyGuard.verify(idempotencyKey);
        return actionRepository.findByIdempotencyKey(command.actorId(), key)
                .map(action -> replay(command, action))
                .orElseGet(() -> createAction(command, key));
    }

    private ServiceRestartResult createAction(RestartServiceCommand command, UUID idempotencyKey) {
        validate(command);
        serviceConfigRepository.findByName(command.serviceName())
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_CONFIG_NOT_FOUND));
        confirmationPort.verifyTotp(command.actorId(), command.confirmationCode(), idempotencyKey);

        Instant now = Instant.now(clock);
        AdminConfigAction action = AdminConfigAction.restartService(
                UUID.randomUUID(),
                command.serviceName(),
                command.reason(),
                ESTIMATED_DOWNTIME_SECONDS,
                idempotencyKey,
                now,
                command.actorId());
        AdminConfigAction saved = actionRepository.save(action);
        auditLogWriter.recordConfigAction(saved, command.auditContext());
        log.info("[ACTION] Complete RestartService | userId={} | service={} | actionId={}",
                LogMaskingUtil.maskId(command.actorId()),
                command.serviceName(),
                LogMaskingUtil.maskId(saved.id()));
        return toRestartResult(saved, false);
    }

    private ServiceRestartResult replay(RestartServiceCommand command, AdminConfigAction action) {
        if (action.actionType() != AdminConfigActionType.RESTART_SERVICE
                || action.serviceName().filter(command.serviceName()::equalsIgnoreCase).isEmpty()) {
            throw new BusinessException(ErrorCode.CONFIG_ACTION_CONFLICT);
        }
        log.info("[ACTION] Replay RestartService | userId={} | actionId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(action.id()));
        return toRestartResult(action, true);
    }

    private void validate(RestartServiceCommand command) {
        if (command.serviceName().isBlank()
                || command.confirmationCode().isBlank()
                || command.reason().length() < MIN_REASON_LENGTH) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
    }

    private ServiceRestartResult toRestartResult(AdminConfigAction action, boolean replayed) {
        return new ServiceRestartResult(
                action.id(),
                action.status(),
                action.estimatedDowntimeSeconds().orElse(ESTIMATED_DOWNTIME_SECONDS),
                action.requestedAt(),
                false,
                replayed);
    }
}
