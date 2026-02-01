package com.chat.application.usecase;

import com.chat.application.dto.ConversationSummary;
import com.chat.application.dto.NotificationDto;
import com.chat.application.port.in.StartDirectConversationUseCase;
import com.chat.application.port.out.ConversationRepositoryPort;
import com.chat.application.port.out.NotificationPublisherRepositoryPort;
import com.chat.domain.exceptions.DomainException;
import com.chat.domain.model.ConversationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StartDirectConversationService implements StartDirectConversationUseCase {
    private static final Logger log = LoggerFactory.getLogger(StartDirectConversationService.class);
    private final ConversationRepositoryPort convs;
    private final NotificationPublisherRepositoryPort notifications;

    public StartDirectConversationService(ConversationRepositoryPort convs,
                                          NotificationPublisherRepositoryPort notifications) {
        this.convs = convs;
        this.notifications = notifications;
    }

    @Override
    public ConversationSummary start(String requester, String peer) {
        if (requester == null || peer == null) throw new DomainException("Utilisateur invalide");
        if (requester.equals(peer)) throw new DomainException("Impossible de discuter avec vous-même");

        String id = convs.getOrCreateDirect(requester, peer);

        // Ceinture/bretelles: garantis l’adhésion (idempotent)
        List<String> members = convs.membersOf(id);
        if (members == null || members.isEmpty()) {
            convs.addMember(id, requester);
            convs.addMember(id, peer);
            members = convs.membersOf(id);
        }

        // Nom côté requester = le “peer”
        String name = members.stream().filter(u -> !u.equals(requester)).findFirst().orElse(peer);

//        NotificationDto notification = NotificationDto.convCreated(id, requester, name);
//        notifications.notifyUser(requester, notification);

        log.info("After create DM id={} members={}", id, members);
        return new ConversationSummary(id, ConversationType.DIRECT, name, members);
    }
}