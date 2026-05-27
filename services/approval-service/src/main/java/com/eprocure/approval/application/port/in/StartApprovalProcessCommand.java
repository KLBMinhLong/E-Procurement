package com.eprocure.approval.application.port.in;

import com.eprocure.approval.domain.model.PurchaseRequestPriority;
import com.eprocure.approval.domain.model.vo.Money;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record StartApprovalProcessCommand(
        String eventId,
        String topic,
        Integer partitionId,
        Long offsetValue,
        UUID traceId,
        UUID purchaseRequestId,
        String prNumber,
        String title,
        UUID requesterId,
        UUID departmentId,
        Money totalAmount,
        Set<String> categories,
        PurchaseRequestPriority priority,
        Map<String, Object> entitySnapshot) {

    public StartApprovalProcessCommand {
        eventId = requireText(eventId, "eventId");
        topic = topic == null || topic.isBlank() ? "procurement.pr.submitted" : topic.trim();
        purchaseRequestId = Objects.requireNonNull(purchaseRequestId, "purchaseRequestId must not be null");
        prNumber = requireText(prNumber, "prNumber");
        title = title == null || title.isBlank() ? prNumber : title.trim();
        requesterId = Objects.requireNonNull(requesterId, "requesterId must not be null");
        departmentId = Objects.requireNonNull(departmentId, "departmentId must not be null");
        totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        priority = Objects.requireNonNull(priority, "priority must not be null");
        categories = categories == null ? Set.of() : Set.copyOf(categories);
        entitySnapshot = entitySnapshot == null ? Map.of() : Map.copyOf(entitySnapshot);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
