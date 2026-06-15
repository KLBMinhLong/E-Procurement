package com.eprocure.admin.application.usecase;

import com.eprocure.admin.application.port.in.RotateEncryptionKeyCommand;
import com.eprocure.admin.application.port.out.IamSecurityConfirmationPort;
import com.eprocure.admin.application.service.EncryptionKeyRotationResult;
import com.eprocure.admin.application.service.IdempotencyGuard;
import com.eprocure.admin.common.exception.BusinessException;
import com.eprocure.admin.common.exception.ErrorCode;
import com.eprocure.admin.common.util.LogMaskingUtil;
import com.eprocure.admin.domain.model.AdminConfigAction;
import com.eprocure.admin.domain.model.AdminConfigActionType;
import com.eprocure.admin.domain.repository.AdminConfigActionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RotateEncryptionKeyUseCase {
    private static final Logger log = LogManager.getLogger(RotateEncryptionKeyUseCase.class);
    private static final DateTimeFormatter KEY_VERSION_FORMATTER =
            DateTimeFormatter.ofPattern("'pending-v'yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final AdminConfigActionRepository actionRepository;
    private final IamSecurityConfirmationPort confirmationPort;
    private final IdempotencyGuard idempotencyGuard;
    private final Clock clock;

    public RotateEncryptionKeyUseCase(
            AdminConfigActionRepository actionRepository,
            IamSecurityConfirmationPort confirmationPort,
            IdempotencyGuard idempotencyGuard,
            Clock clock) {
        this.actionRepository = actionRepository;
        this.confirmationPort = confirmationPort;
        this.idempotencyGuard = idempotencyGuard;
        this.clock = clock;
    }

    @Transactional
    public EncryptionKeyRotationResult execute(RotateEncryptionKeyCommand command, String idempotencyKey) {
        UUID key = idempotencyGuard.verify(idempotencyKey);
        return actionRepository.findByIdempotencyKey(command.actorId(), key)
                .map(action -> replay(command, action))
                .orElseGet(() -> createAction(command, key));
    }

    private EncryptionKeyRotationResult createAction(RotateEncryptionKeyCommand command, UUID idempotencyKey) {
        validate(command);
        confirmationPort.verifyTotp(command.actorId(), command.confirmationCode(), idempotencyKey);
        Instant now = Instant.now(clock);
        AdminConfigAction action = AdminConfigAction.rotateEncryptionKey(
                UUID.randomUUID(),
                KEY_VERSION_FORMATTER.format(now),
                idempotencyKey,
                now,
                command.actorId());
        AdminConfigAction saved = actionRepository.save(action);
        log.info("[ACTION] Complete RotateEncryptionKey | userId={} | actionId={} | keySize={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(saved.id()),
                command.keySize());
        return toRotationResult(saved, false);
    }

    private EncryptionKeyRotationResult replay(RotateEncryptionKeyCommand command, AdminConfigAction action) {
        if (action.actionType() != AdminConfigActionType.ROTATE_ENCRYPTION_KEY) {
            throw new BusinessException(ErrorCode.CONFIG_ACTION_CONFLICT);
        }
        log.info("[ACTION] Replay RotateEncryptionKey | userId={} | actionId={}",
                LogMaskingUtil.maskId(command.actorId()),
                LogMaskingUtil.maskId(action.id()));
        return toRotationResult(action, true);
    }

    private void validate(RotateEncryptionKeyCommand command) {
        if (command.confirmationCode().isBlank()
                || (command.keySize() != 2048 && command.keySize() != 4096)) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
    }

    private EncryptionKeyRotationResult toRotationResult(AdminConfigAction action, boolean replayed) {
        return new EncryptionKeyRotationResult(
                action.id(),
                action.status(),
                action.keyVersion().orElse("pending"),
                action.requestedAt(),
                action.requestedAt().plusSeconds(86_400),
                false,
                replayed);
    }
}
