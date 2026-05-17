package com.eprocure.iam.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

public class PermissionDbEntity {
    public UUID id;
    public String code;
    public String name;
    public String description;
    public String service;
    public Instant createdAt;
}
