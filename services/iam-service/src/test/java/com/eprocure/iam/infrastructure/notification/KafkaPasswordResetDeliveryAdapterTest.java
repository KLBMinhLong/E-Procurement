package com.eprocure.iam.infrastructure.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.iam.domain.model.User;
import com.eprocure.iam.domain.model.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class KafkaPasswordResetDeliveryAdapterTest {
    private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String RAW_TOKEN = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final Instant EXPIRES_AT = Instant.parse("2026-06-01T03:15:00Z");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T03:00:00Z"), ZoneOffset.UTC);

    @Test
    void should_publish_password_reset_email_event_when_kafka_delivery_enabled() {
        FakeApplicationEventPublisher publisher = new FakeApplicationEventPublisher();
        KafkaPasswordResetDeliveryAdapter adapter = new KafkaPasswordResetDeliveryAdapter(
                publisher,
                null,
                CLOCK,
                "notification.email.send",
                "https://app.eprocure.local/reset-password",
                "vi");

        adapter.sendResetInstructions(activeUser(), RAW_TOKEN, EXPIRES_AT);

        assertThat(publisher.event).isInstanceOf(PasswordResetEmailEvent.class);
        PasswordResetEmailEvent event = (PasswordResetEmailEvent) publisher.event;
        assertThat(event.eventType()).isEqualTo("notification.email.send");
        assertThat(event.source()).isEqualTo("iam-service");
        assertThat(event.timestamp()).isEqualTo(CLOCK.instant().toString());
        assertThat(event.payload().templateEventType()).isEqualTo("PASSWORD_RESET");
        assertThat(event.payload().recipientId()).isEqualTo(USER_ID.toString());
        assertThat(event.payload().recipientEmail()).isEqualTo("requester@eprocure.local");
        assertThat(event.payload().resetUrl())
                .isEqualTo("https://app.eprocure.local/reset-password?token=" + RAW_TOKEN);
        assertThat(event.payload().expiresAt()).isEqualTo(EXPIRES_AT.toString());
        assertThat(event.payload().language()).isEqualTo("vi");
        assertThat(event.payload().referenceType()).isEqualTo("USER");
        assertThat(event.payload().referenceId()).isEqualTo(USER_ID.toString());
    }

    private static User activeUser() {
        return User.create(
                USER_ID,
                "EMP-2025-00001",
                "requester",
                "requester@eprocure.local",
                "Request User",
                DEPARTMENT_ID,
                UserStatus.ACTIVE,
                Instant.parse("2026-05-17T00:00:00Z"));
    }

    private static final class FakeApplicationEventPublisher implements ApplicationEventPublisher {
        private Object event;

        @Override
        public void publishEvent(Object event) {
            this.event = event;
        }
    }
}
