package com.eprocure.notification.infrastructure.persistence.mapper;

import com.eprocure.notification.domain.repository.NotificationFilter;
import com.eprocure.notification.infrastructure.persistence.entity.NotificationDbEntity;
import com.eprocure.notification.infrastructure.persistence.entity.NotificationTemplateDbEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper {

    @Insert("""
            INSERT INTO notification.notifications (
                id, recipient_id, event_type, channel, subject, body,
                reference_type, reference_id, reference_number, action_url,
                email_to, provider_message_id, status, is_read, read_at, sent_at,
                retry_count, last_error, last_attempt_at, next_attempt_at,
                created_at, updated_at, created_by, is_deleted, deleted_at, deleted_by
            ) VALUES (
                #{entity.id}, #{entity.recipientId}, #{entity.eventType}, #{entity.channel},
                #{entity.subject}, #{entity.body}, #{entity.referenceType}, #{entity.referenceId},
                #{entity.referenceNumber}, #{entity.actionUrl}, #{entity.emailTo}, #{entity.providerMessageId},
                #{entity.status}, #{entity.read}, #{entity.readAt}, #{entity.sentAt},
                #{entity.retryCount}, #{entity.lastError}, #{entity.lastAttemptAt}, #{entity.nextAttemptAt},
                #{entity.createdAt}, #{entity.updatedAt}, #{entity.createdBy}, #{entity.deleted},
                #{entity.deletedAt}, #{entity.deletedBy}
            )
            """)
    void insert(@Param("entity") NotificationDbEntity entity);

    List<NotificationDbEntity> findByFilter(@Param("filter") NotificationFilter filter);

    long countByFilter(@Param("filter") NotificationFilter filter);

    @Select("""
            SELECT COUNT(*)
            FROM notification.notifications
            WHERE recipient_id = #{recipientId}
              AND is_read = FALSE
              AND is_deleted = FALSE
            """)
    long countUnread(@Param("recipientId") UUID recipientId);

    @Select("""
            SELECT
                id, recipient_id, event_type, channel, subject, body,
                reference_type, reference_id, reference_number, action_url,
                email_to, provider_message_id, status, is_read, read_at, sent_at,
                retry_count, last_error, last_attempt_at, next_attempt_at,
                created_at, updated_at, created_by, is_deleted, deleted_at, deleted_by
            FROM notification.notifications
            WHERE id = #{id}
              AND recipient_id = #{recipientId}
              AND is_deleted = FALSE
            """)
    @Results(id = "notificationResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "recipientId", column = "recipient_id"),
            @Result(property = "eventType", column = "event_type"),
            @Result(property = "channel", column = "channel"),
            @Result(property = "subject", column = "subject"),
            @Result(property = "body", column = "body"),
            @Result(property = "referenceType", column = "reference_type"),
            @Result(property = "referenceId", column = "reference_id"),
            @Result(property = "referenceNumber", column = "reference_number"),
            @Result(property = "actionUrl", column = "action_url"),
            @Result(property = "emailTo", column = "email_to"),
            @Result(property = "providerMessageId", column = "provider_message_id"),
            @Result(property = "status", column = "status"),
            @Result(property = "read", column = "is_read"),
            @Result(property = "readAt", column = "read_at"),
            @Result(property = "sentAt", column = "sent_at"),
            @Result(property = "retryCount", column = "retry_count"),
            @Result(property = "lastError", column = "last_error"),
            @Result(property = "lastAttemptAt", column = "last_attempt_at"),
            @Result(property = "nextAttemptAt", column = "next_attempt_at"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "deleted", column = "is_deleted"),
            @Result(property = "deletedAt", column = "deleted_at"),
            @Result(property = "deletedBy", column = "deleted_by")
    })
    Optional<NotificationDbEntity> findByIdAndRecipient(
            @Param("id") UUID id,
            @Param("recipientId") UUID recipientId);

    @Update("""
            UPDATE notification.notifications
            SET is_read = TRUE,
                read_at = #{readAt},
                updated_at = #{readAt}
            WHERE id = #{id}
              AND recipient_id = #{recipientId}
              AND is_deleted = FALSE
              AND is_read = FALSE
            """)
    int markRead(@Param("id") UUID id, @Param("recipientId") UUID recipientId, @Param("readAt") Instant readAt);

    @Update("""
            UPDATE notification.notifications
            SET is_read = TRUE,
                read_at = #{readAt},
                updated_at = #{readAt}
            WHERE recipient_id = #{recipientId}
              AND is_deleted = FALSE
              AND is_read = FALSE
            """)
    int markAllRead(@Param("recipientId") UUID recipientId, @Param("readAt") Instant readAt);

    @Select("""
            SELECT
                code, event_type, channel, language, subject_template, body_template,
                is_active, created_at, updated_at
            FROM notification.notification_templates
            WHERE event_type = #{eventType}
              AND channel = #{channel}
              AND language = #{language}
              AND is_active = TRUE
              AND is_deleted = FALSE
            """)
    @Results(id = "notificationTemplateResult", value = {
            @Result(property = "code", column = "code"),
            @Result(property = "eventType", column = "event_type"),
            @Result(property = "channel", column = "channel"),
            @Result(property = "language", column = "language"),
            @Result(property = "subjectTemplate", column = "subject_template"),
            @Result(property = "bodyTemplate", column = "body_template"),
            @Result(property = "active", column = "is_active"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Optional<NotificationTemplateDbEntity> findActiveTemplate(
            @Param("eventType") String eventType,
            @Param("channel") String channel,
            @Param("language") String language);

    Optional<NotificationTemplateDbEntity> findTemplateByCode(@Param("code") String code);

    List<NotificationTemplateDbEntity> findTemplates(
            @Param("channel") String channel,
            @Param("eventType") String eventType,
            @Param("language") String language);

    @Update("""
            UPDATE notification.notification_templates
            SET subject_template = #{subjectTemplate},
                body_template = #{bodyTemplate},
                is_active = #{active},
                updated_at = #{updatedAt}
            WHERE code = #{code}
              AND is_deleted = FALSE
            """)
    int updateTemplate(
            @Param("code") String code,
            @Param("subjectTemplate") String subjectTemplate,
            @Param("bodyTemplate") String bodyTemplate,
            @Param("active") boolean active,
            @Param("updatedAt") Instant updatedAt);

    List<NotificationDbEntity> findEmailDispatchCandidates(
            @Param("now") Instant now,
            @Param("limit") int limit,
            @Param("maxAttempts") short maxAttempts);

    @Update("""
            UPDATE notification.notifications
            SET status = 'SENT',
                sent_at = #{sentAt},
                provider_message_id = #{providerMessageId},
                last_error = NULL,
                last_attempt_at = #{sentAt},
                next_attempt_at = NULL,
                updated_at = #{sentAt}
            WHERE id = #{id}
              AND channel = 'EMAIL'
              AND is_deleted = FALSE
            """)
    int markEmailSent(
            @Param("id") UUID id,
            @Param("sentAt") Instant sentAt,
            @Param("providerMessageId") String providerMessageId);

    @Update("""
            UPDATE notification.notifications
            SET status = CASE WHEN retry_count + 1 >= #{maxAttempts} THEN 'FAILED' ELSE 'PENDING' END,
                retry_count = retry_count + 1,
                last_error = #{lastError},
                last_attempt_at = #{attemptedAt},
                next_attempt_at = CASE
                    WHEN retry_count + 1 >= #{maxAttempts} THEN NULL
                    ELSE CAST(#{nextAttemptAt} AS TIMESTAMPTZ)
                END,
                updated_at = #{attemptedAt}
            WHERE id = #{id}
              AND channel = 'EMAIL'
              AND is_deleted = FALSE
            """)
    int markEmailFailed(
            @Param("id") UUID id,
            @Param("attemptedAt") Instant attemptedAt,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("maxAttempts") short maxAttempts,
            @Param("lastError") String lastError);

    @Insert("""
            INSERT INTO notification.email_dispatch_dead_letters (
                notification_id, recipient_email, event_type, failure_reason, retry_count, failed_at
            ) VALUES (
                #{notificationId}, #{recipientEmail}, #{eventType}, #{failureReason}, #{retryCount}, #{failedAt}
            )
            ON CONFLICT (notification_id) DO NOTHING
            """)
    int insertEmailDeadLetter(
            @Param("notificationId") UUID notificationId,
            @Param("recipientEmail") String recipientEmail,
            @Param("eventType") String eventType,
            @Param("failureReason") String failureReason,
            @Param("retryCount") short retryCount,
            @Param("failedAt") Instant failedAt);

    @Insert("""
            INSERT INTO notification.event_processing_log (
                event_id, event_type, source, topic, partition_id, offset_value, handler_name, status
            ) VALUES (
                #{eventId}, #{eventType}, #{source}, #{topic}, #{partitionId}, #{offsetValue}, #{handlerName}, 'PROCESSED'
            )
            ON CONFLICT (event_id) DO NOTHING
            """)
    int insertEventLog(
            @Param("eventId") String eventId,
            @Param("eventType") String eventType,
            @Param("source") String source,
            @Param("topic") String topic,
            @Param("partitionId") Integer partitionId,
            @Param("offsetValue") Long offsetValue,
            @Param("handlerName") String handlerName);
}
