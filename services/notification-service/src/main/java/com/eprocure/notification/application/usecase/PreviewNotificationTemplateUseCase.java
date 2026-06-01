package com.eprocure.notification.application.usecase;

import com.eprocure.notification.application.port.in.PreviewNotificationTemplateCommand;
import com.eprocure.notification.application.service.IdempotencyService;
import com.eprocure.notification.application.service.NotificationTemplatePreviewResult;
import com.eprocure.notification.application.service.NotificationTemplateRenderer;
import com.eprocure.notification.application.service.RenderedNotification;
import com.eprocure.notification.common.exception.BusinessException;
import com.eprocure.notification.common.exception.ErrorCode;
import com.eprocure.notification.common.util.LogMaskingUtil;
import com.eprocure.notification.domain.model.NotificationTemplate;
import com.eprocure.notification.domain.repository.NotificationRepository;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class PreviewNotificationTemplateUseCase {
    private static final Logger log = LogManager.getLogger(PreviewNotificationTemplateUseCase.class);
    private static final String OPERATION = "notification-template:preview";

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRenderer renderer;
    private final IdempotencyService idempotencyService;

    public PreviewNotificationTemplateUseCase(
            NotificationRepository notificationRepository,
            NotificationTemplateRenderer renderer,
            IdempotencyService idempotencyService) {
        this.notificationRepository = notificationRepository;
        this.renderer = renderer;
        this.idempotencyService = idempotencyService;
    }

    public NotificationTemplatePreviewResult execute(PreviewNotificationTemplateCommand command, String idempotencyKey) {
        idempotencyService.verify(idempotencyKey);
        UUID actorId = command.actorId();
        var cached = idempotencyService.find(OPERATION, actorId, idempotencyKey, NotificationTemplatePreviewResult.class);
        if (cached.isPresent()) {
            return new NotificationTemplatePreviewResult(cached.get().subject(), cached.get().htmlBody(), true);
        }

        String code = normalizeCode(command.code());
        log.info("[ACTION] Start PreviewNotificationTemplate | code={} | userId={}",
                code,
                LogMaskingUtil.maskId(actorId));
        NotificationTemplate template = notificationRepository.findTemplateByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.NTF_002));
        RenderedNotification rendered = renderer.render(template, command.variables());
        NotificationTemplatePreviewResult result =
                new NotificationTemplatePreviewResult(rendered.subject(), rendered.body(), false);
        idempotencyService.save(OPERATION, actorId, idempotencyKey, result);
        log.info("[ACTION] Complete PreviewNotificationTemplate | code={} | userId={}",
                code,
                LogMaskingUtil.maskId(actorId));
        return result;
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
        return code.trim().toUpperCase();
    }
}
