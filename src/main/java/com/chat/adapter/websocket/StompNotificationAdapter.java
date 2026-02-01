package com.chat.adapter.websocket;

import com.chat.application.dto.NotificationDto;
import com.chat.application.port.out.NotificationPublisherRepositoryPort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompNotificationAdapter implements NotificationPublisherRepositoryPort {

    private final SimpMessagingTemplate messaging;

    public StompNotificationAdapter(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public void notifyUser(String username, NotificationDto notification) {
        messaging.convertAndSendToUser(
                username,
                "/queue/notifications",
                notification
        );
    }
}
