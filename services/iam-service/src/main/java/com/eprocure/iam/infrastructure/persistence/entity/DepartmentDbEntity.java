package com.eprocure.iam.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

public class DepartmentDbEntity {
    public UUID id;
    public String code;
    public String name;
    public UUID parentId;
    public UUID headUserId;
    public Instant createdAt;
    public Instant updatedAt;
    public Boolean deleted;
}
