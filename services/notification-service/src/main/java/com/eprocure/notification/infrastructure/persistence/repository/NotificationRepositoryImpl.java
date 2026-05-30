package com.eprocure.notification.infrastructure.persistence.repository;

import com.eprocure.notification.domain.model.Notification;
import com.eprocure.notification.domain.model.NotificationChannel;
import com.eprocure.notification.domain.model.NotificationTemplate;
import com.eprocure.notification.domain.repository.NotificationFilter;
import com.eprocure.notification.domain.repository.NotificationRepository;
import com.eprocure.notification.infrastructure.persistence.entity.NotificationDbEntity;
import com.eprocure.notification.infrastructure.persistence.entity.NotificationTemplateDbEntity;
import com.eprocure.notification.infrastructure.persistence.mapper.NotificationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepositoryImpl implements NotificationRepository {
    private static final Logger log = LogManager.getLogger(NotificationRepositoryImpl.class);

    private final NotificationMapper mapper;
    private final ObjectMapper objectMapper;

    public NotificationRepositoryImpl(
            NotificationMapper mapper,
            @Qualifier("domainObjectMapper") ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(Notification notification) {
        log.debug("[REPO] insert notification | id={}", notification.id());
        mapper.insert(objectMapper.convertValue(notification, NotificationDbEntity.class));
    }

    @Override
    public List<Notification> findByFilter(NotificationFilter filter) {
        log.debug("[REPO] findByFilter notifications | recipientId={}", filter.recipientId());
        return mapper.findByFilter(filter).stream()
                .map(entity -> objectMapper.convertValue(entity, Notification.class))
                .toList();
    }

    @Override
    public long countByFilter(NotificationFilter filter) {
        return mapper.countByFilter(filter);
    }

    @Override
    public long countUnread(UUID recipientId) {
        return mapper.countUnread(recipientId);
    }

    @Override
    public Optional<Notification> findByIdAndRecipient(UUID id, UUID recipientId) {
        return mapper.findByIdAndRecipient(id, recipientId)
                .map(entity -> objectMapper.convertValue(entity, Notification.class));
    }

    @Override
    public int markRead(UUID id, UUID recipientId, Instant readAt) {
        return mapper.markRead(id, recipientId, readAt);
    }

    @Override
    public int markAllRead(UUID recipientId, Instant readAt) {
        return mapper.markAllRead(recipientId, readAt);
    }

    @Override
    public Optional<NotificationTemplate> findActiveTemplate(
            String eventType,
            NotificationChannel channel,
            String language) {
        return mapper.findActiveTemplate(eventType, channel.name(), language)
                .map(this::toTemplate);
    }

    @Override
    public List<Notification> findEmailDispatchCandidates(Instant now, int limit, short maxAttempts) {
        return mapper.findEmailDispatchCandidates(now, limit, maxAttempts).stream()
                .map(entity -> objectMapper.convertValue(entity, Notification.class))
                .toList();
    }

    @Override
    public int markEmailSent(UUID id, Instant sentAt, String providerMessageId) {
        return mapper.markEmailSent(id, sentAt, providerMessageId);
    }

    @Override
    public int markEmailFailed(
            UUID id,
            Instant attemptedAt,
            Instant nextAttemptAt,
            short maxAttempts,
            String lastError) {
        return mapper.markEmailFailed(id, attemptedAt, nextAttemptAt, maxAttempts, lastError);
    }

    @Override
    public int recordEmailDeadLetter(
            UUID notificationId,
            String recipientEmail,
            String eventType,
            String failureReason,
            short retryCount,
            Instant failedAt) {
        return mapper.insertEmailDeadLetter(notificationId, recipientEmail, eventType, failureReason, retryCount, failedAt);
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
        return mapper.insertEventLog(eventId, eventType, source, topic, partitionId, offsetValue, handlerName) == 1;
    }

    private NotificationTemplate toTemplate(NotificationTemplateDbEntity entity) {
        return objectMapper.convertValue(entity, NotificationTemplate.class);
    }
}
