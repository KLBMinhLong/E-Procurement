package com.eprocure.notification.application.usecase;

import com.eprocure.notification.application.port.out.EmailSendCommand;
import com.eprocure.notification.application.port.out.EmailSendResult;
import com.eprocure.notification.application.port.out.EmailSenderPort;
import com.eprocure.notification.common.util.LogMaskingUtil;
import com.eprocure.notification.domain.model.Notification;
import com.eprocure.notification.domain.repository.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DispatchPendingEmailsUseCase {
    private static final Logger log = LogManager.getLogger(DispatchPendingEmailsUseCase.class);
    private static final int MAX_ERROR_LENGTH = 500;
    private static final Pattern SENSITIVE_ASSIGNMENT =
            Pattern.compile("(?i)(password|token|secret|api[-_]?key|apikey|authorization)=([^\\s&]+)");

    private final NotificationRepository notificationRepository;
    private final EmailSenderPort emailSenderPort;
    private final Clock clock;
    private final short maxAttempts;
    private final long retryDelaySeconds;
    private final int batchSize;

    public DispatchPendingEmailsUseCase(
            NotificationRepository notificationRepository,
            EmailSenderPort emailSenderPort,
            Clock clock,
            @Value("${eprocure.notification.email.max-attempts:3}") int maxAttempts,
            @Value("${eprocure.notification.email.retry-delay-seconds:30}") long retryDelaySeconds,
            @Value("${eprocure.notification.email.batch-size:25}") int batchSize) {
        this.notificationRepository = notificationRepository;
        this.emailSenderPort = emailSenderPort;
        this.clock = clock;
        this.maxAttempts = (short) Math.max(1, Math.min(maxAttempts, Short.MAX_VALUE));
        this.retryDelaySeconds = Math.max(1, retryDelaySeconds);
        this.batchSize = Math.max(1, batchSize);
    }

    @Transactional
    public int execute() {
        Instant now = clock.instant();
        List<Notification> candidates = notificationRepository.findEmailDispatchCandidates(now, batchSize, maxAttempts);
        for (Notification notification : candidates) {
            dispatch(notification, now);
        }
        return candidates.size();
    }

    private void dispatch(Notification notification, Instant attemptedAt) {
        try {
            EmailSendResult result = emailSenderPort.send(new EmailSendCommand(
                    notification.id(),
                    notification.emailTo(),
                    subject(notification),
                    notification.body()));
            notificationRepository.markEmailSent(notification.id(), attemptedAt, result.providerMessageId());
            log.info("[EMAIL] Dispatch complete | notificationId={} | to={}",
                    LogMaskingUtil.maskId(notification.id()),
                    LogMaskingUtil.maskEmail(notification.emailTo()));
        } catch (RuntimeException exception) {
            String sanitizedError = sanitizeError(exception);
            short nextRetryCount = (short) (notification.retryCount() + 1);
            Instant nextAttemptAt = attemptedAt.plusSeconds(retryDelaySeconds);
            notificationRepository.markEmailFailed(
                    notification.id(),
                    attemptedAt,
                    nextAttemptAt,
                    maxAttempts,
                    sanitizedError);
            if (nextRetryCount >= maxAttempts) {
                notificationRepository.recordEmailDeadLetter(
                        notification.id(),
                        notification.emailTo(),
                        notification.eventType(),
                        sanitizedError,
                        nextRetryCount,
                        attemptedAt);
            }
            log.warn("[EMAIL] Dispatch failed | notificationId={} | to={} | attempt={}/{}",
                    LogMaskingUtil.maskId(notification.id()),
                    LogMaskingUtil.maskEmail(notification.emailTo()),
                    nextRetryCount,
                    maxAttempts);
        }
    }

    private String subject(Notification notification) {
        return notification.subject() == null || notification.subject().isBlank()
                ? notification.eventType()
                : notification.subject();
    }

    private String sanitizeError(RuntimeException exception) {
        String rawMessage = exception.getMessage();
        String message = rawMessage == null || rawMessage.isBlank()
                ? exception.getClass().getSimpleName()
                : rawMessage;
        String sanitized = SENSITIVE_ASSIGNMENT.matcher(message).replaceAll("$1=***");
        if (sanitized.length() > MAX_ERROR_LENGTH) {
            return sanitized.substring(0, MAX_ERROR_LENGTH);
        }
        return sanitized;
    }
}
