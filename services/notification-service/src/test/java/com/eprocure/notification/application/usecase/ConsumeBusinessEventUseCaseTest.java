package com.eprocure.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.notification.application.port.in.BusinessEventCommand;
import com.eprocure.notification.application.port.out.WebSocketPushPort;
import com.eprocure.notification.application.service.NotificationRecipientResolver;
import com.eprocure.notification.application.service.NotificationTemplateRenderer;
import com.eprocure.notification.application.service.NotificationView;
import com.eprocure.notification.domain.model.Notification;
import com.eprocure.notification.domain.model.NotificationChannel;
import com.eprocure.notification.domain.model.NotificationTemplate;
import com.eprocure.notification.domain.repository.NotificationFilter;
import com.eprocure.notification.domain.repository.NotificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsumeBusinessEventUseCaseTest {
    private static final UUID RECIPIENT_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID BUDGET_ID = UUID.fromString("70000000-0000-0000-0000-000000000103");
    private static final Instant NOW = Instant.parse("2026-05-30T10:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final FakeNotificationRepository repository = new FakeNotificationRepository();
    private final FakeWebSocketPushPort webSocketPushPort = new FakeWebSocketPushPort();
    private final ConsumeBusinessEventUseCase useCase = new ConsumeBusinessEventUseCase(
            repository,
            new NotificationRecipientResolver(RECIPIENT_ID.toString()),
            new NotificationTemplateRenderer(repository, "vi"),
            webSocketPushPort,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void should_create_budget_warning_notification_when_budget_event_received() throws Exception {
        int created = useCase.execute(command("evt-budget-warning-001"));

        assertThat(created).isEqualTo(1);
        assertThat(repository.saved).hasSize(1);
        Notification notification = repository.saved.get(0);
        assertThat(notification.recipientId()).isEqualTo(RECIPIENT_ID);
        assertThat(notification.eventType()).isEqualTo("BUDGET_WARNING");
        assertThat(notification.referenceType()).isEqualTo("BUDGET");
        assertThat(notification.referenceId()).isEqualTo(BUDGET_ID);
        assertThat(notification.actionUrl()).isEqualTo("/finance/budgets/" + BUDGET_ID);
        assertThat(notification.body()).contains("100000000.0000");
        assertThat(webSocketPushPort.pushed).hasSize(1);
    }

    @Test
    void should_skip_duplicate_event_when_event_already_processed() throws Exception {
        BusinessEventCommand command = command("evt-budget-warning-002");

        int first = useCase.execute(command);
        int second = useCase.execute(command);

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        assertThat(repository.saved).hasSize(1);
        assertThat(webSocketPushPort.pushed).hasSize(1);
    }

    @Test
    void should_create_pending_email_notification_when_email_send_event_received() throws Exception {
        int created = useCase.execute(emailCommand("evt-email-password-reset-001"));

        assertThat(created).isEqualTo(1);
        assertThat(repository.saved).hasSize(1);
        Notification notification = repository.saved.get(0);
        assertThat(notification.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(notification.status().name()).isEqualTo("PENDING");
        assertThat(notification.emailTo()).isEqualTo("requester@example.com");
        assertThat(notification.eventType()).isEqualTo("PASSWORD_RESET");
        assertThat(notification.body()).contains("https://app.eprocure.local/reset");
        assertThat(webSocketPushPort.pushed).isEmpty();
    }

    @Test
    void should_create_purchase_order_notification_when_po_issued_event_received() throws Exception {
        UUID poId = UUID.fromString("96000000-0000-4000-8000-000000000001");

        int created = useCase.execute(poIssuedCommand("evt-po-issued-001", poId));

        assertThat(created).isEqualTo(1);
        Notification notification = repository.saved.get(0);
        assertThat(notification.recipientId()).isEqualTo(RECIPIENT_ID);
        assertThat(notification.eventType()).isEqualTo("PO_ISSUED");
        assertThat(notification.referenceType()).isEqualTo("PURCHASE_ORDER");
        assertThat(notification.referenceId()).isEqualTo(poId);
        assertThat(notification.referenceNumber()).isEqualTo("PO-2026-000001");
        assertThat(notification.actionUrl()).isEqualTo("/finance/purchase-orders/" + poId);
        assertThat(webSocketPushPort.pushed).hasSize(1);
    }

    private BusinessEventCommand command(String eventId) throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "alertType": "WARNING",
                  "budgetId": "70000000-0000-0000-0000-000000000103",
                  "departmentId": "33333333-3333-3333-3333-333333333333",
                  "fiscalYear": 2026,
                  "glAccountCode": "6002",
                  "projectedAvailable": {
                    "amount": "100000000.0000",
                    "currency": "VND"
                  },
                  "impactAmount": {
                    "amount": "700000000.0000",
                    "currency": "VND"
                  },
                  "referenceType": "PURCHASE_REQUEST",
                  "referenceId": "88000000-0000-0000-0000-000000000001",
                  "reason": "Budget available after this request is below 20% threshold"
                }
                """);
        return new BusinessEventCommand(
                eventId,
                "FINANCE_BUDGET_WARNING",
                "finance-service",
                NOW,
                "finance.budget.warning",
                0,
                12L,
                payload);
    }

    private BusinessEventCommand emailCommand(String eventId) throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "recipientId": "10000000-0000-4000-8000-000000000001",
                  "recipientEmail": "requester@example.com",
                  "templateEventType": "PASSWORD_RESET",
                  "resetUrl": "https://app.eprocure.local/reset?token=opaque-short-lived",
                  "language": "vi"
                }
                """);
        return new BusinessEventCommand(
                eventId,
                "notification.email.send",
                "iam-service",
                NOW,
                "notification.email.send",
                0,
                30L,
                payload);
    }

    private BusinessEventCommand poIssuedCommand(String eventId, UUID poId) throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "poId": "%s",
                  "poNumber": "PO-2026-000001",
                  "recipientId": "%s",
                  "referenceType": "PURCHASE_ORDER",
                  "referenceId": "%s",
                  "referenceNumber": "PO-2026-000001",
                  "vendorName": "Acme Supplier",
                  "totalAmount": "25000000.0000",
                  "currency": "VND"
                }
                """.formatted(poId, RECIPIENT_ID, poId));
        return new BusinessEventCommand(
                eventId,
                "PO_ISSUED",
                "finance-service",
                NOW,
                "procurement.po.issued",
                0,
                13L,
                payload);
    }

    private static final class FakeWebSocketPushPort implements WebSocketPushPort {
        private final List<NotificationView> pushed = new ArrayList<>();

        @Override
        public void push(NotificationView notification) {
            pushed.add(notification);
        }
    }

    private static final class FakeNotificationRepository implements NotificationRepository {
        private final Set<String> processedEvents = new HashSet<>();
        private final List<Notification> saved = new ArrayList<>();

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
            if ("PASSWORD_RESET".equals(eventType) && channel == NotificationChannel.EMAIL) {
                return Optional.of(new NotificationTemplate(
                        "EMAIL_PASSWORD_RESET_VI",
                        "PASSWORD_RESET",
                        NotificationChannel.EMAIL,
                        "vi",
                        "Reset password",
                        "Open {{resetUrl}} to reset your password.",
                        true,
                        NOW,
                        NOW));
            }
            if ("PO_ISSUED".equals(eventType) && channel == NotificationChannel.IN_APP) {
                return Optional.of(new NotificationTemplate(
                        "IN_APP_PO_ISSUED_VI",
                        "PO_ISSUED",
                        NotificationChannel.IN_APP,
                        "vi",
                        "PO {{poNumber}} issued",
                        "PO {{poNumber}} for {{vendorName}} has been issued.",
                        true,
                        NOW,
                        NOW));
            }
            if (!"BUDGET_WARNING".equals(eventType) || channel != NotificationChannel.IN_APP) {
                return Optional.empty();
            }
            return Optional.of(new NotificationTemplate(
                    "IN_APP_BUDGET_WARNING_VI",
                    "BUDGET_WARNING",
                    NotificationChannel.IN_APP,
                    "vi",
                    "Budget {{glAccountCode}} warning",
                    "Projected available {{projectedAvailable.amount}} {{projectedAvailable.currency}}",
                    true,
                    NOW,
                    NOW));
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
            return processedEvents.add(eventId);
        }
    }
}
