package com.eprocure.notification.infrastructure.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.eprocure.notification.application.port.in.BusinessEventCommand;
import com.eprocure.notification.application.usecase.ConsumeBusinessEventUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

class BusinessEventNotificationConsumerTest {

    @Test
    void should_parse_numeric_epoch_seconds_timestamp_when_iam_kafka_serializer_sends_instant() {
        CapturingConsumeBusinessEventUseCase useCase = new CapturingConsumeBusinessEventUseCase();
        BusinessEventNotificationConsumer consumer = new BusinessEventNotificationConsumer(useCase, new ObjectMapper());

        consumer.consume(record("""
                {
                  "eventId": "evt-password-reset-001",
                  "eventType": "notification.email.send",
                  "source": "iam-service",
                  "timestamp": 1780303230.703440068,
                  "payload": {
                    "templateEventType": "PASSWORD_RESET",
                    "recipientId": "10000000-0000-4000-8000-000000000001",
                    "recipientEmail": "requester@example.com",
                    "resetUrl": "https://app.eprocure.local/reset-password?token=opaque",
                    "language": "vi"
                  }
                }
                """));

        assertThat(useCase.command).isPresent();
        assertThat(useCase.command.get().timestamp())
                .isEqualTo(Instant.ofEpochSecond(1780303230L, 703440000L));
        assertThat(useCase.command.get().topic()).isEqualTo("notification.email.send");
    }

    @Test
    void should_skip_invalid_event_without_throwing_when_timestamp_missing() {
        CapturingConsumeBusinessEventUseCase useCase = new CapturingConsumeBusinessEventUseCase();
        BusinessEventNotificationConsumer consumer = new BusinessEventNotificationConsumer(useCase, new ObjectMapper());

        assertThatCode(() -> consumer.consume(record("""
                {
                  "eventId": "evt-password-reset-002",
                  "eventType": "notification.email.send",
                  "source": "iam-service",
                  "payload": {
                    "recipientId": "10000000-0000-4000-8000-000000000001",
                    "recipientEmail": "requester@example.com"
                  }
                }
                """))).doesNotThrowAnyException();

        assertThat(useCase.command).isEmpty();
    }

    private ConsumerRecord<String, String> record(String value) {
        return new ConsumerRecord<>("notification.email.send", 2, 0L, "key", value);
    }

    private static final class CapturingConsumeBusinessEventUseCase extends ConsumeBusinessEventUseCase {
        private Optional<BusinessEventCommand> command = Optional.empty();

        private CapturingConsumeBusinessEventUseCase() {
            super(null, null, null, null, null);
        }

        @Override
        public int execute(BusinessEventCommand command) {
            this.command = Optional.of(command);
            return 1;
        }
    }
}
