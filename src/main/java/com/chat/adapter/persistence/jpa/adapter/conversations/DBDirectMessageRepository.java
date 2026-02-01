package com.chat.adapter.persistence.jpa.adapter.conversations;

import com.chat.adapter.persistence.jpa.entity.ConversationEntity;
import com.chat.adapter.persistence.jpa.repository.conversations.ConversationJpaRepository;
import com.chat.application.dto.ConversationSummary;
import com.chat.application.port.out.ConversationRepositoryPort;
import com.chat.domain.model.ConversationType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Profile("inDB")
public class DBDirectMessageRepository implements ConversationRepositoryPort {

    private final ConversationJpaRepository jpaRepository;

    public DBDirectMessageRepository(ConversationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public String getOrCreateDirect(String userA, String userB) {
        // 1. Chercher si une conversation DIRECT existe déjà entre A et B
        List<ConversationEntity> userConvs = jpaRepository.findByParticipant(userA);

        Optional<ConversationEntity> existing = userConvs.stream()
                .filter(c -> c.getType() == ConversationType.DIRECT)
                .filter(c -> c.getParticipants().contains(userB))
                .findFirst();

        if (existing.isPresent()) {
            return existing.get().getId();
        }

        // 2. Créer une nouvelle conversation
        ConversationEntity newConv = new ConversationEntity();
        newConv.setId(UUID.randomUUID().toString());
        newConv.setType(ConversationType.DIRECT);
        newConv.setName(userA + " & " + userB); // Nom par défaut

        newConv.getParticipants().add(userA);
        newConv.getParticipants().add(userB);

        jpaRepository.save(newConv);

        return newConv.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMember(String conversationId, String username) {
        if ("public".equals(conversationId)) return true;

        return jpaRepository.findById(conversationId)
                .map(c -> c.getParticipants().contains(username))
                .orElse(false);
    }

    @Override
    @Transactional
    public void addMember(String conversationId, String username) {
        jpaRepository.findById(conversationId).ifPresent(c -> {
            c.getParticipants().add(username);
            jpaRepository.save(c);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> membersOf(String conversationId) {
        if ("public".equals(conversationId)) return Collections.emptyList();

        Optional<ConversationEntity> opt = jpaRepository.findById(conversationId);

        return opt.isPresent() ? new ArrayList<>(opt.get().getParticipants()) : Collections.emptyList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationSummary> listForUser(String username) {
        List<ConversationEntity> entities = jpaRepository.findByParticipant(username);

        return entities.stream()
                .map(e -> {
                    String displayName = e.getName();

                    // Si DM -> on affiche le nom de l'AUTRE personne (peer)
                    if (e.getType() == ConversationType.DIRECT) {
                        for (String p : e.getParticipants()) {
                            if (!p.equals(username)) {
                                displayName = p;
                                break;
                            }
                        }
                    }

                    // Conversion Set -> List pour correspondre au DTO
                    List<String> membersList = new ArrayList<>(e.getParticipants());

                    return new ConversationSummary(
                            e.getId(),
                            e.getType(), // Enum ConversationType
                            displayName,
                            membersList
                    );
                })
                .collect(Collectors.toList());
    }
}
