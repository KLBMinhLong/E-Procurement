package com.eprocure.notification.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.notification.application.event.NotificationPushRequestedEvent;
import com.eprocure.notification.application.service.NotificationView;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class StompWebSocketPushAdapterTest {
    private static final UUID RECIPIENT_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID NOTIFICATION_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");

    private RecordingApplicationEventPublisher applicationEventPublisher;
    private RecordingSimpMessagingTemplate messagingTemplate;
    private StompWebSocketPushAdapter adapter;

    @BeforeEach
    void setUp() {
        applicationEventPublisher = new RecordingApplicationEventPublisher();
        messagingTemplate = new RecordingSimpMessagingTemplate();
        adapter = new StompWebSocketPushAdapter(applicationEventPublisher, messagingTemplate);
    }

    @Test
    void should_publish_application_event_when_push_called() {
        adapter.push(notificationView());

        assertThat(applicationEventPublisher.publishedEvent)
                .isInstanceOf(NotificationPushRequestedEvent.class);
    }

    @Test
    void should_send_notification_to_user_queue_after_commit() {
        adapter.afterCommit(new NotificationPushRequestedEvent(notificationView(), Instant.now()));

        assertThat(messagingTemplate.user).isEqualTo(RECIPIENT_ID.toString());
        assertThat(messagingTemplate.destination).isEqualTo("/queue/notifications");
        assertThat(messagingTemplate.payload).isInstanceOf(NotificationPushPayload.class);
    }

    @Test
    void should_not_throw_when_websocket_send_fails_after_commit() {
        messagingTemplate.failSend = true;

        assertThatCode(() -> adapter.afterCommit(new NotificationPushRequestedEvent(notificationView(), Instant.now())))
                .doesNotThrowAnyException();
    }

    private NotificationView notificationView() {
        return new NotificationView(
                NOTIFICATION_ID,
                RECIPIENT_ID,
                "BUDGET_WARNING",
                "IN_APP",
                "Budget warning",
                "Budget is below threshold",
                false,
                null,
                "BUDGET",
                UUID.fromString("30000000-0000-4000-8000-000000000001"),
                "BUD-2026-001",
                "/finance/budgets",
                Instant.parse("2026-05-30T10:00:00Z"));
    }

    private static final class RecordingApplicationEventPublisher implements ApplicationEventPublisher {
        private Object publishedEvent;

        @Override
        public void publishEvent(Object event) {
            this.publishedEvent = event;
        }
    }

    private static final class RecordingSimpMessagingTemplate extends SimpMessagingTemplate {
        private String user;
        private String destination;
        private Object payload;
        private boolean failSend;

        private RecordingSimpMessagingTemplate() {
            super(new NoopMessageChannel());
        }

        @Override
        public void convertAndSendToUser(String user, String destination, Object payload) {
            if (failSend) {
                throw new IllegalStateException("broker unavailable");
            }
            this.user = user;
            this.destination = destination;
            this.payload = payload;
        }
    }

    private static final class NoopMessageChannel implements MessageChannel {
        @Override
        public boolean send(Message<?> message, long timeout) {
            return true;
        }
    }
}
