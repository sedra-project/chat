package com.chat.adapter.websocket;

import com.chat.adapter.websocket.dto.requests.SimplePrincipal;
import com.chat.application.port.out.ConversationRepositoryPort;
import com.chat.application.port.out.SessionTokenRepositoryPort;
import com.chat.application.port.out.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);

    private final SessionTokenRepositoryPort tokens;
    private final ConversationRepositoryPort conversations;
    private final UserRepositoryPort users;

    public StompAuthChannelInterceptor(SessionTokenRepositoryPort tokens,
                                       ConversationRepositoryPort conversations,
                                       UserRepositoryPort users) {
        this.tokens = tokens;
        this.conversations = conversations;
        this.users = users;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        // 1) Authentification à la connexion
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String auth = accessor.getFirstNativeHeader("Authorization");
            String token = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : null;

            String userId = (token != null) ? tokens.findUserIdByToken(token).orElse(null) : null;
            if (userId == null) {
                throw new MessagingException("Unauthorized");
            }

            String username = users.findById(userId)
                    .map(u -> u.getUsername().value())
                    .orElse(null);

            log.info("STOMP CONNECT Authorization header present? {} userId={} username={}", auth != null, userId, username);
            if (username == null) {
                throw new MessagingException("Unauthorized");
            }

            // Principal = userId (stable)
            accessor.setUser(new SimplePrincipal(userId));

            // Optimisation: on mémorise le username dans la session STOMP (évite DB lookup à chaque frame)
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put("username", username);
            }
        }

        // 2) Contrôle d’accès SUBSCRIBE
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String dest = accessor.getDestination();
            if (dest != null && dest.startsWith("/topic/conv.")) {
                String convId = extractConversationId(dest);

                String userId = accessor.getUser() != null ? accessor.getUser().getName() : null;
                if (userId == null) throw new MessagingException("Unauthorized");

                String username = getUsernameFromSession(accessor);
                if (username == null) {
                    // fallback (au cas où) : lookup DB
                    username = users.findById(userId)
                            .map(u -> u.getUsername().value())
                            .orElseThrow(() -> new MessagingException("Unauthorized"));
                }

                boolean member = conversations.isMember(convId, username);
                log.info("SUBSCRIBE dest={} convId={} userId={} username={} member={}", dest, convId, userId, username, member);

                if (!member) {
                    throw new MessagingException("Forbidden: not a member of conversation " + convId);
                }
            }
        }

        // 3) Contrôle d’accès SEND
        if (StompCommand.SEND.equals(accessor.getCommand())) {
            String dest = accessor.getDestination();

            String userId = accessor.getUser() != null ? accessor.getUser().getName() : null;
            if (userId == null) throw new MessagingException("Unauthorized");

            String username = getUsernameFromSession(accessor);
            if (username == null) {
                username = users.findById(userId)
                        .map(u -> u.getUsername().value())
                        .orElseThrow(() -> new MessagingException("Unauthorized"));
            }

            if (dest != null && dest.startsWith("/app/conv.")) {
                String convId = extractConversationId(dest);

                boolean member = conversations.isMember(convId, username);
                log.info("SEND dest={} convId={} userId={} username={} member={}", dest, convId, userId, username, member);

                if (!member) {
                    throw new MessagingException("Forbidden: not a member of conversation " + convId);
                }
            }
        }

        return message;
    }

    private String getUsernameFromSession(StompHeaderAccessor accessor) {
        if (accessor.getSessionAttributes() == null) return null;
        Object v = accessor.getSessionAttributes().get("username");
        return (v instanceof String) ? (String) v : null;
    }

    /**
     * Nettoie la destination pour extraire uniquement l'ID de la conversation.
     * Gère : /topic/conv.1001, /topic/conv.1001.reactions, /app/conv.1001.message
     */
    private String extractConversationId(String destination) {
        if (destination == null) return null;

        String s;
        if (destination.startsWith("/topic/conv.")) {
            s = destination.substring("/topic/conv.".length());
        } else if (destination.startsWith("/app/conv.")) {
            s = destination.substring("/app/conv.".length());
        } else {
            return null;
        }

        int q = s.indexOf('?');
        if (q > 0) s = s.substring(0, q);
        int slash = s.indexOf('/');
        if (slash > 0) s = s.substring(0, slash);

        if (s.endsWith(".reactions")) {
            return s.substring(0, s.length() - ".reactions".length());
        } else if (s.endsWith(".reaction")) {
            return s.substring(0, s.length() - ".reaction".length());
        } else if (s.endsWith(".message")) {
            return s.substring(0, s.length() - ".message".length());
        }

        int dotIndex = s.indexOf('.');
        if (dotIndex > 0) {
            return s.substring(0, dotIndex);
        }

        return s;
    }
}