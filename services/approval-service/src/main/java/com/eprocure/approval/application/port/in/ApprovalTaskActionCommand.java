package com.eprocure.approval.application.port.in;

import com.eprocure.approval.domain.model.ApprovalAction;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ApprovalTaskActionCommand(
        String taskId,
        UUID actorId,
        ApprovalAction action,
        String comment,
        List<String> requestedFields,
        UUID forwardToUserId) {

    public ApprovalTaskActionCommand {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
        taskId = taskId.trim();
        actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        action = Objects.requireNonNull(action, "action must not be null");
        comment = comment == null || comment.isBlank() ? null : comment.trim();
        requestedFields = requestedFields == null ? List.of() : List.copyOf(requestedFields);
    }
}
