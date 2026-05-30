package com.eprocure.notification.application.service;

import com.eprocure.notification.domain.model.NotificationChannel;
import com.eprocure.notification.domain.model.NotificationTemplate;
import com.eprocure.notification.domain.repository.NotificationRepository;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificationTemplateRenderer {
    private static final Logger log = LogManager.getLogger(NotificationTemplateRenderer.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");

    private final NotificationRepository notificationRepository;
    private final String defaultLanguage;

    public NotificationTemplateRenderer(
            NotificationRepository notificationRepository,
            @Value("${eprocure.notification.default-language:vi}") String defaultLanguage) {
        this.notificationRepository = notificationRepository;
        this.defaultLanguage = defaultLanguage == null || defaultLanguage.isBlank()
                ? "vi"
                : defaultLanguage.trim().toLowerCase();
    }

    public RenderedNotification render(
            String eventType,
            NotificationChannel channel,
            String language,
            Map<String, String> variables) {
        String preferredLanguage = language == null || language.isBlank() ? defaultLanguage : language.trim().toLowerCase();
        Optional<NotificationTemplate> template =
                notificationRepository.findActiveTemplate(eventType, channel, preferredLanguage)
                        .or(() -> notificationRepository.findActiveTemplate(eventType, channel, defaultLanguage));
        if (template.isEmpty()) {
            log.warn("[ACTION] Notification template missing | eventType={} | channel={}", eventType, channel);
            return defaultRendered(eventType, variables);
        }
        return new RenderedNotification(
                renderTemplate(template.get().subjectTemplate(), variables),
                renderTemplate(template.get().bodyTemplate(), variables));
    }

    private RenderedNotification defaultRendered(String eventType, Map<String, String> variables) {
        String referenceNumber = variables.getOrDefault("referenceNumber", variables.getOrDefault("prNumber", ""));
        String body = referenceNumber.isBlank() ? eventType : eventType + " - " + referenceNumber;
        return new RenderedNotification(eventType, body);
    }

    private String renderTemplate(String template, Map<String, String> variables) {
        if (template == null || template.isBlank()) {
            return null;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            matcher.appendReplacement(builder, Matcher.quoteReplacement(safe(variables.getOrDefault(key, ""))));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
