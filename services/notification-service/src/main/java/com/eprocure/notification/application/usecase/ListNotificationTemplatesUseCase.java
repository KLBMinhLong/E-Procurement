package com.eprocure.notification.application.usecase;

import com.eprocure.notification.application.port.in.ListNotificationTemplatesQuery;
import com.eprocure.notification.application.service.NotificationTemplateView;
import com.eprocure.notification.domain.repository.NotificationRepository;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class ListNotificationTemplatesUseCase {
    private static final Logger log = LogManager.getLogger(ListNotificationTemplatesUseCase.class);

    private final NotificationRepository notificationRepository;

    public ListNotificationTemplatesUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<NotificationTemplateView> execute(ListNotificationTemplatesQuery query) {
        log.info("[ACTION] Start ListNotificationTemplates | channel={} | eventType={} | language={}",
                query.channel(),
                query.eventType(),
                query.language());
        return notificationRepository.findTemplates(query.channel(), query.eventType(), query.language()).stream()
                .map(NotificationTemplateView::from)
                .toList();
    }
}
