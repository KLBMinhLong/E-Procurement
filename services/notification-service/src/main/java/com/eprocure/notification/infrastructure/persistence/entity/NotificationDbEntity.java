package com.eprocure.notification.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

public class NotificationDbEntity {
    public UUID id;
    public UUID recipientId;
    public String eventType;
    public String channel;
    public String subject;
    public String body;
    public String referenceType;
    public UUID referenceId;
    public String referenceNumber;
    public String actionUrl;
    public String emailTo;
    public String providerMessageId;
    public String status;
    public boolean read;
    public Instant readAt;
    public Instant sentAt;
    public short retryCount;
    public String lastError;
    public Instant lastAttemptAt;
    public Instant nextAttemptAt;
    public Instant createdAt;
    public Instant updatedAt;
    public UUID createdBy;
    public boolean deleted;
    public Instant deletedAt;
    public UUID deletedBy;
}
