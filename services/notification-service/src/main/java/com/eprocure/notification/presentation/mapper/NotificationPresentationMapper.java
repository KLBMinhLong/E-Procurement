package com.eprocure.notification.presentation.mapper;

import com.eprocure.notification.application.port.in.ListNotificationsQuery;
import com.eprocure.notification.application.service.NotificationView;
import com.eprocure.notification.common.security.UserPrincipal;
import com.eprocure.notification.presentation.response.NotificationResponse;
import org.springframework.stereotype.Component;

@Component
public class NotificationPresentationMapper {

    public ListNotificationsQuery toListQuery(
            UserPrincipal principal,
            Boolean read,
            String eventType,
            int page,
            int size) {
        return new ListNotificationsQuery(
                principal.getId(),
                read,
                eventType,
                page,
                size);
    }

    public NotificationResponse toResponse(NotificationView view) {
        return new NotificationResponse(
                view.id(),
                view.eventType(),
                view.channel(),
                view.subject(),
                view.body(),
                view.read(),
                view.readAt(),
                view.referenceType(),
                view.referenceId(),
                view.referenceNumber(),
                view.actionUrl(),
                view.createdAt());
    }
}
