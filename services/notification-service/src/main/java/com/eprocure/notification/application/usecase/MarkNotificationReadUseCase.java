package com.eprocure.notification.application.usecase;

import com.eprocure.notification.application.service.IdempotencyService;
import com.eprocure.notification.application.service.NotificationReadResult;
import com.eprocure.notification.application.service.NotificationView;
import com.eprocure.notification.common.exception.BusinessException;
import com.eprocure.notification.common.exception.ErrorCode;
import com.eprocure.notification.common.util.LogMaskingUtil;
import com.eprocure.notification.domain.model.Notification;
import com.eprocure.notification.domain.repository.NotificationRepository;
import java.time.Clock;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarkNotificationReadUseCase {
    private static final Logger log = LogManager.getLogger(MarkNotificationReadUseCase.class);
    private static final String OPERATION = "notification:mark-read";

    private final NotificationRepository notificationRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public MarkNotificationReadUseCase(
            NotificationRepository notificationRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.notificationRepository = notificationRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public NotificationReadResult execute(UUID notificationId, UUID recipientId, String idempotencyKey) {
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(OPERATION, recipientId, idempotencyKey, NotificationView.class);
        if (cached.isPresent()) {
            return new NotificationReadResult(cached.get(), true);
        }

        log.info("[ACTION] Start MarkNotificationRead | notificationId={} | userId={}",
                LogMaskingUtil.maskId(notificationId),
                LogMaskingUtil.maskId(recipientId));
        Notification notification = notificationRepository.findByIdAndRecipient(notificationId, recipientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NTF_001));
        Notification updated = notification.markRead(clock.instant());
        if (!notification.read()) {
            notificationRepository.markRead(notificationId, recipientId, updated.readAt());
        }
        NotificationView view = NotificationView.from(updated);
        idempotencyService.save(OPERATION, recipientId, idempotencyKey, view);
        log.info("[ACTION] Complete MarkNotificationRead | notificationId={} | userId={}",
                LogMaskingUtil.maskId(notificationId),
                LogMaskingUtil.maskId(recipientId));
        return new NotificationReadResult(view, false);
    }
}
