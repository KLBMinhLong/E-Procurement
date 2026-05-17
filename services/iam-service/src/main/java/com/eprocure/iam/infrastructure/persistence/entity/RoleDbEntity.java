package com.eprocure.iam.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

public class RoleDbEntity {
    public UUID id;
    public String code;
    public String name;
    public String description;
    public boolean systemRole;
    public Instant createdAt;
}
