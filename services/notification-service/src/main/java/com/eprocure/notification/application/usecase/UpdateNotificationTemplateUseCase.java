package com.eprocure.notification.application.usecase;

import com.eprocure.notification.application.port.in.UpdateNotificationTemplateCommand;
import com.eprocure.notification.application.service.IdempotencyService;
import com.eprocure.notification.application.service.NotificationTemplateUpdateResult;
import com.eprocure.notification.application.service.NotificationTemplateView;
import com.eprocure.notification.common.exception.BusinessException;
import com.eprocure.notification.common.exception.ErrorCode;
import com.eprocure.notification.common.util.LogMaskingUtil;
import com.eprocure.notification.domain.model.NotificationTemplate;
import com.eprocure.notification.domain.repository.NotificationRepository;
import java.time.Clock;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateNotificationTemplateUseCase {
    private static final Logger log = LogManager.getLogger(UpdateNotificationTemplateUseCase.class);
    private static final String OPERATION = "notification-template:update";

    private final NotificationRepository notificationRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public UpdateNotificationTemplateUseCase(
            NotificationRepository notificationRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.notificationRepository = notificationRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public NotificationTemplateUpdateResult execute(UpdateNotificationTemplateCommand command, String idempotencyKey) {
        idempotencyService.verify(idempotencyKey);
        UUID actorId = command.actorId();
        var cached = idempotencyService.find(OPERATION, actorId, idempotencyKey, NotificationTemplateView.class);
        if (cached.isPresent()) {
            return new NotificationTemplateUpdateResult(cached.get(), true);
        }

        String code = normalizeCode(command.code());
        validate(command);
        log.info("[ACTION] Start UpdateNotificationTemplate | code={} | userId={}",
                code,
                LogMaskingUtil.maskId(actorId));

        NotificationTemplate existing = notificationRepository.findTemplateByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.NTF_002));
        int updated = notificationRepository.updateTemplate(
                code,
                normalizeOptional(command.subjectTemplate()),
                command.bodyTemplate().trim(),
                command.active(),
                clock.instant());
        if (updated != 1) {
            throw new BusinessException(ErrorCode.NTF_002);
        }
        NotificationTemplateView view = notificationRepository.findTemplateByCode(existing.code())
                .map(NotificationTemplateView::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NTF_002));
        idempotencyService.save(OPERATION, actorId, idempotencyKey, view);
        log.info("[ACTION] Complete UpdateNotificationTemplate | code={} | userId={}",
                code,
                LogMaskingUtil.maskId(actorId));
        return new NotificationTemplateUpdateResult(view, false);
    }

    private void validate(UpdateNotificationTemplateCommand command) {
        if (command.bodyTemplate() == null || command.bodyTemplate().isBlank()) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
        return code.trim().toUpperCase();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
