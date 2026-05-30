package com.eprocure.notification.application.usecase;

import com.eprocure.notification.common.util.LogMaskingUtil;
import com.eprocure.notification.domain.repository.NotificationRepository;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CountUnreadNotificationsUseCase {
    private static final Logger log = LogManager.getLogger(CountUnreadNotificationsUseCase.class);

    private final NotificationRepository notificationRepository;

    public CountUnreadNotificationsUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public long execute(UUID recipientId) {
        log.info("[ACTION] CountUnreadNotifications | userId={}", LogMaskingUtil.maskId(recipientId));
        return notificationRepository.countUnread(recipientId);
    }
}
