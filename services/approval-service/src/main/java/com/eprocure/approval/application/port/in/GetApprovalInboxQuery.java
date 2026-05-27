package com.eprocure.approval.application.port.in;

import java.util.Optional;
import java.util.UUID;

/**
 * Query parameters for the approval inbox.
 * All filter fields are optional; page/size have sensible defaults.
 */
public record GetApprovalInboxQuery(
        UUID approverId,
        int page,
        int size,
        String sort,
        String priority,
        String entityType,
        String minAmount,
        Boolean isOverdue) {

    public GetApprovalInboxQuery {
        if (page < 1) page = 1;
        if (size < 1 || size > 50) size = 20;
        if (sort == null || sort.isBlank()) sort = "slaDeadline,asc";
    }

    public Optional<String> priorityOpt() {
        return Optional.ofNullable(priority).filter(s -> !s.isBlank());
    }

    public Optional<String> entityTypeOpt() {
        return Optional.ofNullable(entityType).filter(s -> !s.isBlank());
    }

    public Optional<String> minAmountOpt() {
        return Optional.ofNullable(minAmount).filter(s -> !s.isBlank());
    }

    public Optional<Boolean> isOverdueOpt() {
        return Optional.ofNullable(isOverdue);
    }

    public int offset() {
        return (page - 1) * size;
    }
}
