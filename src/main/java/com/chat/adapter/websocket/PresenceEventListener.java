package com.chat.adapter.websocket;

import com.chat.adapter.websocket.dto.ChatMessageDto;
import com.chat.application.port.out.ActiveUserRepositoryPort;
import com.chat.application.port.out.UserRepositoryPort;
import com.chat.application.port.out.UsernameReservationPort;
import com.chat.domain.model.ChatMessage;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.Instant;

@Component
public class PresenceEventListener {

    private final ActiveUserRepositoryPort activeUsers;
    private final UsernameReservationPort reservations;
    private final SimpMessagingTemplate messaging;
    private final UserRepositoryPort users;

    public PresenceEventListener(ActiveUserRepositoryPort activeUsers,
                                 UsernameReservationPort reservations,
                                 SimpMessagingTemplate messaging,
                                 UserRepositoryPort users) {
        this.activeUsers = activeUsers;
        this.reservations = reservations;
        this.messaging = messaging;
        this.users = users;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        Principal p = event.getUser();
        if (p == null) return;

        String userId = p.getName(); // <-- maintenant c'est userId

        String username = users.findById(userId)
                .map(u -> u.getUsername().value())
                .orElse(null);

        if (username == null) return;

        reservations.release(username);

        boolean added = activeUsers.add(username);
        if (added) {
            ChatMessage join = ChatMessage.join("public", username, Instant.now());
            messaging.convertAndSend("/topic/public", ChatMessageDto.from("public", join));
            messaging.convertAndSend("/topic/users", activeUsers.list());
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Principal p = event.getUser();
        if (p == null) return;

        String userId = p.getName();

        String username = users.findById(userId)
                .map(u -> u.getUsername().value())
                .orElse(null);

        if (username == null) return;

        boolean removed = activeUsers.remove(username);
        if (removed) {
            ChatMessage leave = ChatMessage.leave("public", username, Instant.now());
            messaging.convertAndSend("/topic/public", ChatMessageDto.from("public", leave));
            messaging.convertAndSend("/topic/users", activeUsers.list());
        }
    }
}