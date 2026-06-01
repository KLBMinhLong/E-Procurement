package com.eprocure.notification.infrastructure.persistence.entity;

import java.time.Instant;

public class NotificationTemplateDbEntity {
    public String code;
    public String eventType;
    public String channel;
    public String language;
    public String subjectTemplate;
    public String bodyTemplate;
    public boolean active;
    public Instant createdAt;
    public Instant updatedAt;
}
