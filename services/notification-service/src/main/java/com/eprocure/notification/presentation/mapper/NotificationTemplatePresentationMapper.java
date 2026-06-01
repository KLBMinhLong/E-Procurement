package com.eprocure.notification.presentation.mapper;

import com.eprocure.notification.application.port.in.ListNotificationTemplatesQuery;
import com.eprocure.notification.application.port.in.PreviewNotificationTemplateCommand;
import com.eprocure.notification.application.port.in.UpdateNotificationTemplateCommand;
import com.eprocure.notification.application.service.NotificationTemplatePreviewResult;
import com.eprocure.notification.application.service.NotificationTemplateView;
import com.eprocure.notification.common.exception.BusinessException;
import com.eprocure.notification.common.exception.ErrorCode;
import com.eprocure.notification.common.security.UserPrincipal;
import com.eprocure.notification.domain.model.NotificationChannel;
import com.eprocure.notification.presentation.request.UpdateNotificationTemplateRequest;
import com.eprocure.notification.presentation.response.NotificationTemplatePreviewResponse;
import com.eprocure.notification.presentation.response.NotificationTemplateResponse;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationTemplatePresentationMapper {

    public ListNotificationTemplatesQuery toListQuery(String channel, String eventType, String language) {
        return new ListNotificationTemplatesQuery(
                parseChannel(channel),
                normalizeUpper(eventType),
                normalizeLower(language));
    }

    public UpdateNotificationTemplateCommand toUpdateCommand(
            String code,
            UpdateNotificationTemplateRequest request,
            UserPrincipal principal) {
        return new UpdateNotificationTemplateCommand(
                code,
                request.subjectTemplate(),
                request.bodyTemplate(),
                Boolean.TRUE.equals(request.isActive()),
                principal.getId());
    }

    public PreviewNotificationTemplateCommand toPreviewCommand(
            String code,
            Map<String, Object> sampleData,
            UserPrincipal principal) {
        Map<String, String> variables = new LinkedHashMap<>();
        if (sampleData != null) {
            sampleData.forEach((key, value) -> variables.put(key, value == null ? "" : String.valueOf(value)));
        }
        return new PreviewNotificationTemplateCommand(code, variables, principal.getId());
    }

    public NotificationTemplateResponse toResponse(NotificationTemplateView view) {
        return new NotificationTemplateResponse(
                view.code(),
                view.eventType(),
                view.channel(),
                view.language(),
                view.subjectTemplate(),
                view.bodyTemplate(),
                view.active(),
                view.updatedAt());
    }

    public NotificationTemplatePreviewResponse toResponse(NotificationTemplatePreviewResult result) {
        return new NotificationTemplatePreviewResponse(result.subject(), result.htmlBody());
    }

    private NotificationChannel parseChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return null;
        }
        try {
            return NotificationChannel.valueOf(channel.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VAL_001);
        }
    }

    private String normalizeUpper(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeLower(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
