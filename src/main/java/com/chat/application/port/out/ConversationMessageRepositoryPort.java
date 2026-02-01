package com.chat.application.port.out;

import com.chat.domain.model.ChatMessage;

import java.util.List;
import java.util.Optional;

/**
 * Port de sortie pour la gestion de la persistance des messages
 * au sein des conversations privées ou de groupes.
 */
public interface ConversationMessageRepositoryPort {

    /**
     * Ajoute un nouveau message à une conversation spécifique.
     */
    void append(String conversationId, ChatMessage message);

    /**
     * Récupère les messages les plus récents d'une conversation.
     *
     * @param max Le nombre maximum de messages à retourner.
     */
    List<ChatMessage> recent(String conversationId, int max);

    /**
     * Recherche un message spécifique par son ID au sein d'une conversation.
     * Crucial pour la gestion des réactions et des modifications.
     */
    Optional<ChatMessage> findById(String conversationId, String messageId);

    void update(ChatMessage message);
}