package com.eprocure.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.notification.application.port.in.PreviewNotificationTemplateCommand;
import com.eprocure.notification.application.port.in.UpdateNotificationTemplateCommand;
import com.eprocure.notification.application.service.IdempotencyService;
import com.eprocure.notification.application.service.NotificationTemplateRenderer;
import com.eprocure.notification.domain.model.Notification;
import com.eprocure.notification.domain.model.NotificationChannel;
import com.eprocure.notification.domain.model.NotificationTemplate;
import com.eprocure.notification.domain.repository.NotificationFilter;
import com.eprocure.notification.domain.repository.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationTemplateAdminUseCaseTest {
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-06-01T04:00:00Z");
    private static final String IDEMPOTENCY_KEY = "00000000-0000-4000-8000-000000000001";

    @Test
    void should_update_template_when_command_is_valid() {
        FakeNotificationRepository repository = new FakeNotificationRepository(template());
        FakeIdempotencyService idempotencyService = new FakeIdempotencyService();
        UpdateNotificationTemplateUseCase useCase = new UpdateNotificationTemplateUseCase(
                repository,
                idempotencyService,
                Clock.fixed(NOW, ZoneOffset.UTC));

        var result = useCase.execute(
                new UpdateNotificationTemplateCommand(
                        "email_password_reset_vi",
                        "Updated subject {{name}}",
                        "Updated body {{resetUrl}}",
                        false,
                        ACTOR_ID),
                IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(result.template().code()).isEqualTo("EMAIL_PASSWORD_RESET_VI");
        assertThat(result.template().subjectTemplate()).isEqualTo("Updated subject {{name}}");
        assertThat(result.template().bodyTemplate()).isEqualTo("Updated body {{resetUrl}}");
        assertThat(result.template().active()).isFalse();
        assertThat(result.template().updatedAt()).isEqualTo(NOW);
        assertThat(idempotencyService.savedOperation).isEqualTo("notification-template:update");
        assertThat(idempotencyService.savedActorId).isEqualTo(ACTOR_ID);
    }

    @Test
    void should_render_preview_without_persisting_notification() {
        FakeNotificationRepository repository = new FakeNotificationRepository(template());
        FakeIdempotencyService idempotencyService = new FakeIdempotencyService();
        PreviewNotificationTemplateUseCase useCase = new PreviewNotificationTemplateUseCase(
                repository,
                new NotificationTemplateRenderer(repository, "vi"),
                idempotencyService);

        var result = useCase.execute(
                new PreviewNotificationTemplateCommand(
                        "EMAIL_PASSWORD_RESET_VI",
                        Map.of("name", "<Requester>", "resetUrl", "https://app/reset"),
                        ACTOR_ID),
                IDEMPOTENCY_KEY);

        assertThat(result.replayed()).isFalse();
        assertThat(result.subject()).isEqualTo("Reset &lt;Requester&gt;");
        assertThat(result.htmlBody()).contains("https://app/reset");
        assertThat(repository.saved).isEmpty();
        assertThat(idempotencyService.savedOperation).isEqualTo("notification-template:preview");
        assertThat(idempotencyService.savedActorId).isEqualTo(ACTOR_ID);
    }

    private NotificationTemplate template() {
        return new NotificationTemplate(
                "EMAIL_PASSWORD_RESET_VI",
                "PASSWORD_RESET",
                NotificationChannel.EMAIL,
                "vi",
                "Reset {{name}}",
                "Open {{resetUrl}} to reset password.",
                true,
                NOW.minusSeconds(60),
                NOW.minusSeconds(60));
    }

    private static final class FakeIdempotencyService extends IdempotencyService {
        private String savedOperation;
        private UUID savedActorId;

        private FakeIdempotencyService() {
            super(null, null);
        }

        @Override
        public void verify(String idempotencyKey) {
            assertThat(idempotencyKey).isEqualTo(IDEMPOTENCY_KEY);
        }

        @Override
        public <T> Optional<T> find(String operation, UUID actorId, String idempotencyKey, Class<T> type) {
            return Optional.empty();
        }

        @Override
        public void save(String operation, UUID actorId, String idempotencyKey, Object response) {
            this.savedOperation = operation;
            this.savedActorId = actorId;
        }
    }

    private static final class FakeNotificationRepository implements NotificationRepository {
        private final List<Notification> saved = new ArrayList<>();
        private NotificationTemplate template;

        private FakeNotificationRepository(NotificationTemplate template) {
            this.template = template;
        }

        @Override
        public void save(Notification notification) {
            saved.add(notification);
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
            return Optional.ofNullable(template)
                    .filter(value -> value.eventType().equals(eventType)
                            && value.channel() == channel
                            && value.language().equals(language)
                            && value.active());
        }

        @Override
        public Optional<NotificationTemplate> findTemplateByCode(String code) {
            return Optional.ofNullable(template).filter(value -> value.code().equals(code));
        }

        @Override
        public List<NotificationTemplate> findTemplates(NotificationChannel channel, String eventType, String language) {
            return template == null ? List.of() : List.of(template);
        }

        @Override
        public int updateTemplate(
                String code,
                String subjectTemplate,
                String bodyTemplate,
                boolean active,
                Instant updatedAt) {
            if (template == null || !template.code().equals(code)) {
                return 0;
            }
            template = new NotificationTemplate(
                    template.code(),
                    template.eventType(),
                    template.channel(),
                    template.language(),
                    subjectTemplate,
                    bodyTemplate,
                    active,
                    template.createdAt(),
                    updatedAt);
            return 1;
        }

        @Override
        public List<Notification> findEmailDispatchCandidates(Instant now, int limit, short maxAttempts) {
            return List.of();
        }

        @Override
        public int markEmailSent(UUID id, Instant sentAt, String providerMessageId) {
            return 0;
        }

        @Override
        public int markEmailFailed(
                UUID id,
                Instant attemptedAt,
                Instant nextAttemptAt,
                short maxAttempts,
                String lastError) {
            return 0;
        }

        @Override
        public int recordEmailDeadLetter(
                UUID notificationId,
                String recipientEmail,
                String eventType,
                String failureReason,
                short retryCount,
                Instant failedAt) {
            return 0;
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
