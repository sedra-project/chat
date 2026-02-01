package com.chat.adapter.persistence.inmemory;

import com.chat.application.dto.NotificationDto;
import com.chat.application.port.out.NotificationPublisherRepositoryPort;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("inmem")
public class InMemoryNotificationPublisherRepository implements NotificationPublisherRepositoryPort {
    private final SimpMessagingTemplate messaging;

    public InMemoryNotificationPublisherRepository(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public void notifyUser(String username, NotificationDto notification) {
        messaging.convertAndSendToUser(username, "/queue/notifications", notification);
    }
}
