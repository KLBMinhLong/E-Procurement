package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.port.in.ConfigVariableChange;
import com.eprocure.admin.application.port.in.UpdateServiceConfigCommand;
import com.eprocure.admin.application.port.out.IamSecurityConfirmationPort;
import com.eprocure.admin.application.service.IdempotencyGuard;
import com.eprocure.admin.application.service.ServiceConfigUpdateResult;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.domain.model.AdminConfigAction;
import com.eprocure.admin.domain.model.AdminConfigActionType;
import com.eprocure.admin.domain.repository.AdminConfigActionRepository;
import com.eprocure.admin.domain.repository.ServiceConfigRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateServiceConfigUseCase {
    private static final Logger log = LogManager.getLogger(UpdateServiceConfigUseCase.class);
    private static final int MIN_REASON_LENGTH = 10;

    private final ServiceConfigRepository serviceConfigRepository;
    private final AdminConfigActionRepository actionRepository;
    private final IamSecurityConfirmationPort confirmationPort;
    private final IdempotencyGuard idempotencyGuard;
    private final Clock clock;

    public UpdateServiceConfigUseCase(
            ServiceConfigRepository serviceConfigRepository,
            AdminConfigActionRepository actionRepository,
            IamSecurityConfirmationPort confirmationPort,
            IdempotencyGuard idempotencyGuard,
            Clock clock) {
        this.serviceConfigRepository = serviceConfigRepository;
        this.actionRepository = actionRepository;
        this.confirmationPort = confirmationPort;
        this.idempotencyGuard = idempotencyGuard;
        this.clock = clock;
    }

    @Transactional
    public ServiceConfigUpdateResult execute(UpdateServiceConfigCommand command, String idempotencyKey) {
        UUID key = idempotencyGuard.verify(idempotencyKey);
        return actionRepository.findByIdempotencyKey(command.actorId(), key)
                .map(action -> replay(command, action))
                .orElseGet(() -> createAction(command, key));
    }

    private ServiceConfigUpdateResult createAction(UpdateServiceConfigCommand command, UUID idempotencyKey) {
        validate(command);
        serviceConfigRepository.findByName(command.serviceName())
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_CONFIG_NOT_FOUND));
        confirmationPort.verifyTotp(command.actorId(), command.confirmationCode(), idempotencyKey);

        Instant now = Instant.now(clock);
        AdminConfigAction action = AdminConfigAction.updateConfig(
                UUID.randomUUID(),
                command.serviceName(),
                command.changeReason(),
                command.variables().size(),
                command.requiresRestart(),
                idempotencyKey,
                now,
                command.actorId());
        AdminConfigAction saved = actionRepository.save(action);
        log.info("[ACTION] Complete UpdateServiceConfig | userId={} | service={} | actionId={} | variableCount={} | requiresRestart={}",
                LogMaskingUtil.maskId(command.actorId()),
                command.serviceName(),
                LogMaskingUtil.maskId(saved.id()),
                saved.variableCount(),
                saved.requiresRestart());
        return toUpdateResult(saved, false);
    }

    private ServiceConfigUpdateResult replay(UpdateServiceConfigCommand command, AdminConfigAction action) {
        if (action.actionType() != AdminConfigActionType.UPDATE_CONFIG
                || action.serviceName().filter(command.serviceName()::equalsIgnoreCase).isEmpty()) {
            throw new BusinessException(ErrorCode.CONFIG_ACTION_CONFLICT);
        }
        log.info("[ACTION] Replay UpdateServiceConfig | userId={} | actionId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(action.id()));
        return toUpdateResult(action, true);
    }

    private void validate(UpdateServiceConfigCommand command) {
        if (command.serviceName().isBlank()
                || command.variables().isEmpty()
                || command.confirmationCode().isBlank()
                || command.changeReason().length() < MIN_REASON_LENGTH) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
        Set<String> keys = new HashSet<>();
        for (ConfigVariableChange variable : command.variables()) {
            String normalizedKey = variable.key().toUpperCase(Locale.ROOT);
            if (normalizedKey.isBlank() || !keys.add(normalizedKey)) {
                throw new BusinessException(ErrorCode.VAL_001);
            }
        }
    }

    private ServiceConfigUpdateResult toUpdateResult(AdminConfigAction action, boolean replayed) {
        return new ServiceConfigUpdateResult(
                action.id(),
                action.status(),
                action.variableCount(),
                action.requiresRestart(),
                action.serviceName().orElse(""),
                false,
                replayed);
    }
}
