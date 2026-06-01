package com.eprocure.notification.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificationRecipientResolver {
    private static final Logger log = LogManager.getLogger(NotificationRecipientResolver.class);
    private static final Set<String> EXPLICIT_RECIPIENT_FIELDS = Set.of(
            "recipientId",
            "approverId",
            "requesterId",
            "delegateId",
            "userId",
            "assigneeId",
            "managerId");

    private final Set<UUID> budgetAlertRecipients;

    public NotificationRecipientResolver(
            @Value("${eprocure.notification.budget-alert-recipient-ids:}") String budgetAlertRecipientIds) {
        this.budgetAlertRecipients = parseRecipients(budgetAlertRecipientIds);
    }

    public Set<UUID> resolve(String eventType, JsonNode payload) {
        Set<UUID> recipients = new LinkedHashSet<>();
        EXPLICIT_RECIPIENT_FIELDS.forEach(field -> addIfPresent(recipients, payload, field));
        if (eventType.startsWith("BUDGET_")) {
            recipients.addAll(budgetAlertRecipients);
        }
        if (recipients.isEmpty()) {
            log.warn("[ACTION] No notification recipient resolved | eventType={}", eventType);
        }
        return Set.copyOf(recipients);
    }

    private void addIfPresent(Set<UUID> recipients, JsonNode payload, String fieldName) {
        JsonNode value = payload.get(fieldName);
        if (value == null || value.isNull() || !value.isTextual()) {
            return;
        }
        try {
            recipients.add(UUID.fromString(value.asText()));
        } catch (IllegalArgumentException exception) {
            log.warn("[ACTION] Invalid recipient id ignored | field={}", fieldName);
        }
    }

    private Set<UUID> parseRecipients(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Set.of();
        }
        Set<UUID> recipients = new LinkedHashSet<>();
        Arrays.stream(rawValue.split("[,;\\s]+"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .forEach(value -> {
                    try {
                        recipients.add(UUID.fromString(value));
                    } catch (IllegalArgumentException exception) {
                        log.warn("[ACTION] Invalid configured notification recipient ignored");
                    }
                });
        return Set.copyOf(recipients);
    }
}
