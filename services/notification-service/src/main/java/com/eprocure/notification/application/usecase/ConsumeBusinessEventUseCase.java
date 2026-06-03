package com.eprocure.notification.application.usecase;

import com.eprocure.notification.application.port.in.BusinessEventCommand;
import com.eprocure.notification.application.port.out.WebSocketPushPort;
import com.eprocure.notification.application.service.NotificationRecipientResolver;
import com.eprocure.notification.application.service.NotificationTemplateRenderer;
import com.eprocure.notification.application.service.NotificationView;
import com.eprocure.notification.application.service.RenderedNotification;
import com.eprocure.notification.common.util.LogMaskingUtil;
import com.eprocure.notification.domain.model.Notification;
import com.eprocure.notification.domain.model.NotificationChannel;
import com.eprocure.notification.domain.repository.NotificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsumeBusinessEventUseCase {
    private static final Logger log = LogManager.getLogger(ConsumeBusinessEventUseCase.class);
    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientResolver recipientResolver;
    private final NotificationTemplateRenderer templateRenderer;
    private final WebSocketPushPort webSocketPushPort;
    private final Clock clock;

    public ConsumeBusinessEventUseCase(
            NotificationRepository notificationRepository,
            NotificationRecipientResolver recipientResolver,
            NotificationTemplateRenderer templateRenderer,
            WebSocketPushPort webSocketPushPort,
            Clock clock) {
        this.notificationRepository = notificationRepository;
        this.recipientResolver = recipientResolver;
        this.templateRenderer = templateRenderer;
        this.webSocketPushPort = webSocketPushPort;
        this.clock = clock;
    }

    @Transactional
    public int execute(BusinessEventCommand command) {
        String eventType = normalizedEventType(command);
        boolean freshEvent = notificationRepository.markEventProcessed(
                command.eventId(),
                eventType,
                command.source(),
                command.topic(),
                command.partitionId(),
                command.offsetValue(),
                "notification-service");
        if (!freshEvent) {
            log.info("[ACTION] Skip duplicate notification event | eventId={}", command.eventId());
            return 0;
        }

        Map<String, String> variables = flatten(command.payload());
        variables.put("eventType", eventType);
        variables.put("source", command.source());
        Set<UUID> recipients = recipientResolver.resolve(eventType, command.payload());
        if (recipients.isEmpty()) {
            return 0;
        }
        if (isEmailSendTopic(command.topic())) {
            return createEmailNotifications(eventType, recipients, variables);
        }

        RenderedNotification rendered = templateRenderer.render(
                eventType,
                NotificationChannel.IN_APP,
                variables.getOrDefault("language", "vi"),
                variables);
        int created = 0;
        for (UUID recipientId : recipients) {
            Notification notification = Notification.createInApp(
                    recipientId,
                    eventType,
                    rendered.subject(),
                    rendered.body(),
                    referenceType(eventType, variables),
                    referenceId(eventType, variables),
                    referenceNumber(variables),
                    actionUrl(eventType, variables),
                    clock.instant());
            notificationRepository.save(notification);
            webSocketPushPort.push(NotificationView.from(notification));
            created++;
            log.info("[ACTION] Created notification | eventType={} | recipientId={}",
                    eventType,
                    LogMaskingUtil.maskId(recipientId));
        }
        return created;
    }

    private String normalizedEventType(BusinessEventCommand command) {
        return switch (command.topic()) {
            case "notification.email.send" -> emailEventType(command);
            case "finance.budget.warning" -> "BUDGET_WARNING";
            case "finance.budget.exceeded" -> "BUDGET_EXCEEDED";
            case "procurement.po.issued" -> "PO_ISSUED";
            case "finance.invoice.matched" -> "INVOICE_MATCHED";
            case "approval.step.assigned" -> "APPROVAL_TASK_ASSIGNED";
            case "approval.sla.warning" -> "SLA_WARNING";
            case "approval.sla.breached" -> "SLA_BREACHED";
            case "procurement.pr.approved" -> "PR_APPROVED";
            case "procurement.pr.rejected" -> "PR_REJECTED";
            case "procurement.pr.changes-requested" -> "PR_CHANGES_REQUESTED";
            default -> command.eventType();
        };
    }

    private int createEmailNotifications(
            String eventType,
            Set<UUID> recipients,
            Map<String, String> variables) {
        String emailTo = emailTo(variables);
        if (emailTo == null) {
            log.warn("[ACTION] Skip email notification without recipient email | eventType={}", eventType);
            return 0;
        }

        RenderedNotification rendered = renderEmail(eventType, variables);
        int created = 0;
        for (UUID recipientId : recipients) {
            Notification notification = Notification.createEmail(
                    recipientId,
                    emailTo,
                    eventType,
                    rendered.subject(),
                    rendered.body(),
                    referenceType(eventType, variables),
                    referenceId(eventType, variables),
                    referenceNumber(variables),
                    actionUrl(eventType, variables),
                    clock.instant());
            notificationRepository.save(notification);
            created++;
            log.info("[ACTION] Created email notification | eventType={} | recipientId={} | to={}",
                    eventType,
                    LogMaskingUtil.maskId(recipientId),
                    LogMaskingUtil.maskEmail(emailTo));
        }
        return created;
    }

    private RenderedNotification renderEmail(String eventType, Map<String, String> variables) {
        String subject = variables.get("subject");
        String body = variables.get("body");
        if (subject != null && !subject.isBlank() && body != null && !body.isBlank()) {
            return new RenderedNotification(subject, body);
        }
        return templateRenderer.render(
                eventType,
                NotificationChannel.EMAIL,
                variables.getOrDefault("language", "vi"),
                variables);
    }

    private boolean isEmailSendTopic(String topic) {
        return "notification.email.send".equals(topic);
    }

    private String emailEventType(BusinessEventCommand command) {
        String eventType = firstText(command.payload(), "templateEventType", "notificationType", "eventType");
        if (eventType == null || eventType.isBlank()) {
            eventType = command.eventType();
        }
        if (eventType == null || eventType.isBlank() || "notification.email.send".equals(eventType)) {
            eventType = "EMAIL_SEND";
        }
        return eventType.trim()
                .replace('.', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
    }

    private String firstText(JsonNode payload, String... fields) {
        for (String field : fields) {
            JsonNode value = payload.get(field);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private Map<String, String> flatten(JsonNode payload) {
        Map<String, String> variables = new HashMap<>();
        flattenInto("", payload, variables);
        return variables;
    }

    private void flattenInto(String prefix, JsonNode node, Map<String, String> variables) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isValueNode()) {
            variables.put(prefix, node.asText());
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String path = prefix.isBlank() ? field.getKey() : prefix + "." + field.getKey();
                flattenInto(path, field.getValue(), variables);
                if (field.getValue().isValueNode()) {
                    variables.putIfAbsent(field.getKey(), field.getValue().asText());
                }
            }
        }
    }

    private String referenceType(String eventType, Map<String, String> variables) {
        if (eventType.startsWith("BUDGET_")) {
            return "BUDGET";
        }
        if (variables.containsKey("referenceType")) {
            return variables.get("referenceType");
        }
        if (eventType.startsWith("PR_") || variables.containsKey("purchaseRequestId")) {
            return "PURCHASE_REQUEST";
        }
        if (eventType.startsWith("INVOICE_") || variables.containsKey("invoiceId")) {
            return "INVOICE";
        }
        if (eventType.startsWith("PO_") || variables.containsKey("poId")) {
            return "PURCHASE_ORDER";
        }
        if (eventType.startsWith("APPROVAL_") || eventType.startsWith("SLA_")) {
            return "APPROVAL_TASK";
        }
        return null;
    }

    private UUID referenceId(String eventType, Map<String, String> variables) {
        String value = eventType.startsWith("BUDGET_")
                ? firstPresent(variables, "budgetId", "referenceId")
                : firstPresent(variables, "referenceId", "invoiceId", "poId", "purchaseRequestId", "taskId", "budgetId");
        if (value == null || !UUID_PATTERN.matcher(value).matches()) {
            return null;
        }
        return UUID.fromString(value);
    }

    private String referenceNumber(Map<String, String> variables) {
        return firstPresent(variables, "referenceNumber", "prNumber", "poNumber", "invoiceNumber");
    }

    private String emailTo(Map<String, String> variables) {
        String value = firstPresent(variables, "recipientEmail", "toEmail", "email", "to");
        if (value == null || !value.contains("@")) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String actionUrl(String eventType, Map<String, String> variables) {
        String actionUrl = variables.get("actionUrl");
        if (actionUrl != null && actionUrl.startsWith("/") && !actionUrl.startsWith("//")) {
            return actionUrl;
        }
        if (eventType.startsWith("BUDGET_") && variables.containsKey("budgetId")) {
            return "/finance/budgets/" + variables.get("budgetId");
        }
        if (eventType.startsWith("APPROVAL_") || eventType.startsWith("SLA_")) {
            String taskId = variables.get("taskId");
            return taskId == null ? "/approvals/inbox" : "/approvals/tasks/" + taskId;
        }
        if (eventType.startsWith("PR_") && variables.containsKey("purchaseRequestId")) {
            return "/procurement/purchase-requests/" + variables.get("purchaseRequestId");
        }
        if (eventType.startsWith("PO_") && variables.containsKey("poId")) {
            return "/finance/purchase-orders/" + variables.get("poId");
        }
        if (eventType.startsWith("INVOICE_") && variables.containsKey("invoiceId")) {
            return "/finance/invoices/" + variables.get("invoiceId");
        }
        return "/notifications";
    }

    private String firstPresent(Map<String, String> variables, String... keys) {
        for (String key : keys) {
            String value = variables.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
