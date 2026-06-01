package com.eprocure.notification.domain.repository;

import com.eprocure.notification.domain.model.Notification;
import com.eprocure.notification.domain.model.NotificationChannel;
import com.eprocure.notification.domain.model.NotificationTemplate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    void save(Notification notification);

    List<Notification> findByFilter(NotificationFilter filter);

    long countByFilter(NotificationFilter filter);

    long countUnread(UUID recipientId);

    Optional<Notification> findByIdAndRecipient(UUID id, UUID recipientId);

    int markRead(UUID id, UUID recipientId, Instant readAt);

    int markAllRead(UUID recipientId, Instant readAt);

    Optional<NotificationTemplate> findActiveTemplate(String eventType, NotificationChannel channel, String language);

    Optional<NotificationTemplate> findTemplateByCode(String code);

    List<NotificationTemplate> findTemplates(NotificationChannel channel, String eventType, String language);

    int updateTemplate(
            String code,
            String subjectTemplate,
            String bodyTemplate,
            boolean active,
            Instant updatedAt);

    List<Notification> findEmailDispatchCandidates(Instant now, int limit, short maxAttempts);

    int markEmailSent(UUID id, Instant sentAt, String providerMessageId);

    int markEmailFailed(UUID id, Instant attemptedAt, Instant nextAttemptAt, short maxAttempts, String lastError);

    int recordEmailDeadLetter(
            UUID notificationId,
            String recipientEmail,
            String eventType,
            String failureReason,
            short retryCount,
            Instant failedAt);

    boolean markEventProcessed(
            String eventId,
            String eventType,
            String source,
            String topic,
            Integer partitionId,
            Long offsetValue,
            String handlerName);
}
