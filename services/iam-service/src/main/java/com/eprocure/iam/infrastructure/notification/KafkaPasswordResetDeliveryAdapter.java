package com.eprocure.iam.infrastructure.notification;

import com.eprocure.iam.application.port.out.PasswordResetDeliveryPort;
import com.eprocure.iam.common.util.LogMaskingUtil;
import com.eprocure.iam.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(name = "eprocure.iam.notification.password-reset.delivery-mode", havingValue = "kafka")
public class KafkaPasswordResetDeliveryAdapter implements PasswordResetDeliveryPort {
    private static final Logger log = LogManager.getLogger(KafkaPasswordResetDeliveryAdapter.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Clock clock;
    private final String topic;
    private final String frontendUrl;
    private final String language;

    public KafkaPasswordResetDeliveryAdapter(
            ApplicationEventPublisher applicationEventPublisher,
            KafkaTemplate<String, Object> kafkaTemplate,
            Clock clock,
            @Value("${eprocure.iam.notification.password-reset.topic:notification.email.send}") String topic,
            @Value("${eprocure.password-reset.frontend-url:http://localhost:4200/reset-password}") String frontendUrl,
            @Value("${eprocure.iam.notification.password-reset.language:vi}") String language) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
        this.topic = normalize(topic, "notification.email.send");
        this.frontendUrl = normalize(frontendUrl, "http://localhost:4200/reset-password");
        this.language = normalize(language, "vi");
    }

    @Override
    public void sendResetInstructions(User user, String rawToken, Instant expiresAt) {
        PasswordResetEmailEvent event = PasswordResetEmailEvent.create(
                user,
                resetUrl(rawToken),
                expiresAt,
                clock.instant(),
                language);
        applicationEventPublisher.publishEvent(event);
        log.info("[ACTION] PasswordResetEmailQueued | eventId={} | userId={} | email={} | expiresAt={}",
                event.eventId(),
                LogMaskingUtil.maskId(user.getId()),
                LogMaskingUtil.maskEmail(user.getEmail()),
                expiresAt);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPasswordResetEmailRequested(PasswordResetEmailEvent event) {
        try {
            kafkaTemplate.send(topic, event.payload().recipientId(), event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.warn("[KAFKA] Publish password reset email failed | topic={} | eventId={} | error={}",
                                    topic,
                                    event.eventId(),
                                    exception.getMessage());
                        } else {
                            log.info("[KAFKA] Published password reset email | topic={} | eventId={} | partition={} | offset={}",
                                    topic,
                                    event.eventId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (RuntimeException exception) {
            log.warn("[KAFKA] Publish password reset email skipped | topic={} | eventId={} | error={}",
                    topic,
                    event.eventId(),
                    exception.getMessage());
        }
    }

    private String resetUrl(String rawToken) {
        return UriComponentsBuilder.fromUriString(frontendUrl)
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
