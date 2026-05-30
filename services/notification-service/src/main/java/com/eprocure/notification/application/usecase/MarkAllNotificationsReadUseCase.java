package com.eprocure.notification.application.usecase;

import com.eprocure.notification.application.service.IdempotencyService;
import com.eprocure.notification.application.service.MarkAllReadResult;
import com.eprocure.notification.common.util.LogMaskingUtil;
import com.eprocure.notification.domain.repository.NotificationRepository;
import java.time.Clock;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarkAllNotificationsReadUseCase {
    private static final Logger log = LogManager.getLogger(MarkAllNotificationsReadUseCase.class);
    private static final String OPERATION = "notification:mark-all-read";

    private final NotificationRepository notificationRepository;
    private final IdempotencyService idempotencyService;
    private final Clock clock;

    public MarkAllNotificationsReadUseCase(
            NotificationRepository notificationRepository,
            IdempotencyService idempotencyService,
            Clock clock) {
        this.notificationRepository = notificationRepository;
        this.idempotencyService = idempotencyService;
        this.clock = clock;
    }

    @Transactional
    public MarkAllReadResult execute(UUID recipientId, String idempotencyKey) {
        idempotencyService.verify(idempotencyKey);
        var cached = idempotencyService.find(OPERATION, recipientId, idempotencyKey, MarkAllReadResult.class);
        if (cached.isPresent()) {
            return new MarkAllReadResult(cached.get().markedCount(), true);
        }
        log.info("[ACTION] Start MarkAllNotificationsRead | userId={}", LogMaskingUtil.maskId(recipientId));
        int markedCount = notificationRepository.markAllRead(recipientId, clock.instant());
        MarkAllReadResult result = new MarkAllReadResult(markedCount, false);
        idempotencyService.save(OPERATION, recipientId, idempotencyKey, result);
        log.info("[ACTION] Complete MarkAllNotificationsRead | userId={} | markedCount={}",
                LogMaskingUtil.maskId(recipientId),
                markedCount);
        return result;
    }
}
