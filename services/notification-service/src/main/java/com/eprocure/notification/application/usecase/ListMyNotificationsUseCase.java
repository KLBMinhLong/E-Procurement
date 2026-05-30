package com.eprocure.notification.application.usecase;

import com.eprocure.notification.application.port.in.ListNotificationsQuery;
import com.eprocure.notification.application.service.NotificationView;
import com.eprocure.notification.application.service.PageMeta;
import com.eprocure.notification.application.service.PageResult;
import com.eprocure.notification.common.util.LogMaskingUtil;
import com.eprocure.notification.domain.repository.NotificationFilter;
import com.eprocure.notification.domain.repository.NotificationRepository;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListMyNotificationsUseCase {
    private static final Logger log = LogManager.getLogger(ListMyNotificationsUseCase.class);

    private final NotificationRepository notificationRepository;

    public ListMyNotificationsUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<NotificationView> execute(ListNotificationsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        NotificationFilter filter = new NotificationFilter(
                query.recipientId(),
                query.read(),
                query.eventType(),
                query.page(),
                query.size(),
                (query.page() - 1) * query.size());
        log.info("[ACTION] Start ListMyNotifications | userId={} | page={} | size={}",
                LogMaskingUtil.maskId(query.recipientId()),
                query.page(),
                query.size());
        var items = notificationRepository.findByFilter(filter).stream()
                .map(NotificationView::from)
                .toList();
        long total = notificationRepository.countByFilter(filter);
        log.info("[ACTION] Complete ListMyNotifications | userId={} | totalCount={}",
                LogMaskingUtil.maskId(query.recipientId()),
                total);
        return new PageResult<>(items, PageMeta.of(total, query.page(), query.size(), "createdAt,desc"));
    }
}
