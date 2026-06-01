package com.eprocure.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.notification.application.port.out.EmailDispatchException;
import com.eprocure.notification.application.port.out.EmailSendCommand;
import com.eprocure.notification.application.port.out.EmailSendResult;
import com.eprocure.notification.application.port.out.EmailSenderPort;
import com.eprocure.notification.domain.model.Notification;
import com.eprocure.notification.domain.model.NotificationChannel;
import com.eprocure.notification.domain.model.NotificationStatus;
import com.eprocure.notification.domain.model.NotificationTemplate;
import com.eprocure.notification.domain.repository.NotificationFilter;
import com.eprocure.notification.domain.repository.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DispatchPendingEmailsUseCaseTest {
    private static final UUID RECIPIENT_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID NOTIFICATION_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-05-30T10:00:00Z");

    @Test
    void should_mark_email_sent_when_provider_succeeds() {
        FakeNotificationRepository repository = new FakeNotificationRepository(emailNotification((short) 0));
        FakeEmailSenderPort sender = new FakeEmailSenderPort(false);
        DispatchPendingEmailsUseCase useCase = useCase(repository, sender);

        int attempted = useCase.execute();

        assertThat(attempted).isEqualTo(1);
        assertThat(sender.sent).hasSize(1);
        assertThat(repository.sentNotificationId).isEqualTo(NOTIFICATION_ID);
        assertThat(repository.deadLetters).isEmpty();
    }

    @Test
    void should_record_dead_letter_when_max_attempts_exhausted() {
        FakeNotificationRepository repository = new FakeNotificationRepository(emailNotification((short) 2));
        FakeEmailSenderPort sender = new FakeEmailSenderPort(true);
        DispatchPendingEmailsUseCase useCase = useCase(repository, sender);

        int attempted = useCase.execute();

        assertThat(attempted).isEqualTo(1);
        assertThat(repository.failedNotificationId).isEqualTo(NOTIFICATION_ID);
        assertThat(repository.failedRetryCount).isEqualTo((short) 3);
        assertThat(repository.deadLetters).containsExactly(NOTIFICATION_ID);
    }

    private DispatchPendingEmailsUseCase useCase(
            FakeNotificationRepository repository,
            FakeEmailSenderPort sender) {
        return new DispatchPendingEmailsUseCase(
                repository,
                sender,
                Clock.fixed(NOW, ZoneOffset.UTC),
                3,
                30,
                25);
    }

    private Notification emailNotification(short retryCount) {
        return new Notification(
                NOTIFICATION_ID,
                RECIPIENT_ID,
                "PASSWORD_RESET",
                NotificationChannel.EMAIL,
                "Reset password",
                "Open reset link",
                "USER",
                RECIPIENT_ID,
                null,
                "/login",
                "requester@example.com",
                null,
                NotificationStatus.PENDING,
                true,
                null,
                null,
                retryCount,
                null,
                null,
                NOW,
                NOW,
                NOW,
                RECIPIENT_ID,
                false,
                null,
                null);
    }

    private static final class FakeEmailSenderPort implements EmailSenderPort {
        private final boolean fail;
        private final List<EmailSendCommand> sent = new ArrayList<>();

        private FakeEmailSenderPort(boolean fail) {
            this.fail = fail;
        }

        @Override
        public EmailSendResult send(EmailSendCommand command) {
            if (fail) {
                throw new EmailDispatchException("provider token=should-not-leak failed");
            }
            sent.add(command);
            return new EmailSendResult("provider-message-001");
        }
    }

    private static final class FakeNotificationRepository implements NotificationRepository {
        private final List<Notification> candidates;
        private final List<UUID> deadLetters = new ArrayList<>();
        private UUID sentNotificationId;
        private UUID failedNotificationId;
        private short failedRetryCount;

        private FakeNotificationRepository(Notification candidate) {
            this.candidates = List.of(candidate);
        }

        @Override
        public void save(Notification notification) {
        }

        @Override
        public List<Notification> findByFilter(NotificationFilter filter) {
            return List.of();
        }

        @Override
        public long countByFilter(NotificationFilter filter) {
            return 0;
        }

        @Override
        public long countUnread(UUID recipientId) {
            return 0;
        }

        @Override
        public Optional<Notification> findByIdAndRecipient(UUID id, UUID recipientId) {
            return Optional.empty();
        }

        @Override
        public int markRead(UUID id, UUID recipientId, Instant readAt) {
            return 0;
        }

        @Override
        public int markAllRead(UUID recipientId, Instant readAt) {
            return 0;
        }

        @Override
        public Optional<NotificationTemplate> findActiveTemplate(
                String eventType,
                NotificationChannel channel,
                String language) {
            return Optional.empty();
        }

        @Override
        public Optional<NotificationTemplate> findTemplateByCode(String code) {
            return Optional.empty();
        }

        @Override
        public List<NotificationTemplate> findTemplates(NotificationChannel channel, String eventType, String language) {
            return List.of();
        }

        @Override
        public int updateTemplate(
                String code,
                String subjectTemplate,
                String bodyTemplate,
                boolean active,
                Instant updatedAt) {
            return 0;
        }

        @Override
        public List<Notification> findEmailDispatchCandidates(Instant now, int limit, short maxAttempts) {
            return candidates;
        }

        @Override
        public int markEmailSent(UUID id, Instant sentAt, String providerMessageId) {
            this.sentNotificationId = id;
            return 1;
        }

        @Override
        public int markEmailFailed(
                UUID id,
                Instant attemptedAt,
                Instant nextAttemptAt,
                short maxAttempts,
                String lastError) {
            this.failedNotificationId = id;
            this.failedRetryCount = maxAttempts;
            assertThat(lastError).doesNotContain("should-not-leak");
            return 1;
        }

        @Override
        public int recordEmailDeadLetter(
                UUID notificationId,
                String recipientEmail,
                String eventType,
                String failureReason,
                short retryCount,
                Instant failedAt) {
            deadLetters.add(notificationId);
            return 1;
        }

        @Override
        public boolean markEventProcessed(
                String eventId,
                String eventType,
                String source,
                String topic,
                Integer partitionId,
                Long offsetValue,
                String handlerName) {
            return true;
        }
    }
}
