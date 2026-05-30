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
                status, is_read, read_at, sent_at, retry_count, last_error,
                created_at, updated_at, created_by, is_deleted, deleted_at, deleted_by
            ) VALUES (
                #{entity.id}, #{entity.recipientId}, #{entity.eventType}, #{entity.channel},
                #{entity.subject}, #{entity.body}, #{entity.referenceType}, #{entity.referenceId},
                #{entity.referenceNumber}, #{entity.actionUrl}, #{entity.status}, #{entity.read},
                #{entity.readAt}, #{entity.sentAt}, #{entity.retryCount}, #{entity.lastError},
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
                status, is_read, read_at, sent_at, retry_count, last_error,
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
            @Result(property = "status", column = "status"),
            @Result(property = "read", column = "is_read"),
            @Result(property = "readAt", column = "read_at"),
            @Result(property = "sentAt", column = "sent_at"),
            @Result(property = "retryCount", column = "retry_count"),
            @Result(property = "lastError", column = "last_error"),
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
