package com.chat.application.port.out;

import com.chat.application.dto.NotificationDto;

public interface NotificationPublisherRepositoryPort {
    void notifyUser(String username, NotificationDto notification);
}