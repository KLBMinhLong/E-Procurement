package com.eprocure.notification.infrastructure.websocket;

import com.eprocure.notification.application.event.NotificationPushRequestedEvent;
import com.eprocure.notification.application.port.out.WebSocketPushPort;
import com.eprocure.notification.application.service.NotificationView;
import com.eprocure.notification.common.util.LogMaskingUtil;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class StompWebSocketPushAdapter implements WebSocketPushPort {
    private static final Logger log = LogManager.getLogger(StompWebSocketPushAdapter.class);
    private static final String NOTIFICATION_QUEUE = "/queue/notifications";

    private final ApplicationEventPublisher applicationEventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    public StompWebSocketPushAdapter(
            ApplicationEventPublisher applicationEventPublisher,
            SimpMessagingTemplate messagingTemplate) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void push(NotificationView notification) {
        applicationEventPublisher.publishEvent(new NotificationPushRequestedEvent(notification, Instant.now()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void afterCommit(NotificationPushRequestedEvent event) {
        String recipientId = event.notification().recipientId().toString();
        try {
            messagingTemplate.convertAndSendToUser(recipientId, NOTIFICATION_QUEUE,
                    NotificationPushPayload.from(event.notification()));
            log.info("[WEBSOCKET] Push delivered | userId={} | notificationId={}",
                    LogMaskingUtil.maskId(event.notification().recipientId()),
                    LogMaskingUtil.maskId(event.notification().id()));
        } catch (RuntimeException exception) {
            log.warn("[WEBSOCKET] Push failed after commit | userId={} | notificationId={} | reason={}",
                    LogMaskingUtil.maskId(event.notification().recipientId()),
                    LogMaskingUtil.maskId(event.notification().id()),
                    exception.getClass().getSimpleName());
        }
    }
}
