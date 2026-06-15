package com.eprocure.admin.domain.repository;

import com.eprocure.admin.domain.model.AdminConfigAction;
import java.util.Optional;
import java.util.UUID;

public interface AdminConfigActionRepository {
    Optional<AdminConfigAction> findByIdempotencyKey(UUID createdBy, UUID idempotencyKey);

    AdminConfigAction save(AdminConfigAction action);
}
