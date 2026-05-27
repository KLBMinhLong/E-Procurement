package com.eprocure.approval.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface UserResolverPort {
    Optional<UserSummary> getUserById(UUID userId);

    record UserSummary(
            UUID id,
            String fullName,
            String departmentName) {
    }
}
