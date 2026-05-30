package com.eprocure.notification.application.port.out;

import com.eprocure.notification.application.service.NotificationView;

public interface WebSocketPushPort {
    void push(NotificationView notification);
}
