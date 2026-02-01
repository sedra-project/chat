package com.chat.domain.model;

import com.chat.domain.exceptions.DomainException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatMessage {

    private String id;
    private String conversationId;
    private MessageType type;
    private String senderUserId;
    private String senderUsername;

    private String content;

    /**
     * Date de création du message (envoi initial).
     */
    private Instant timestamp;

    /**
     * Non-null si le message a été modifié.
     */
    private Instant editedAt;

    /**
     * Non-null si le message a été supprimé (soft delete).
     */
    private Instant deletedAt;

    /**
     * Pour éviter les NPE.
     */
    private List<Reaction> reactions = new ArrayList<>();

    private ReplyTo replyTo;

    private ChatMessage() {
    }

    // ---------- Factories ----------

    public static ChatMessage chatNew(String conversationId,
                                      String senderUserId,
                                      String senderUsername,
                                      String content,
                                      ReplyTo replyTo) {

        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new DomainException("conversationId requis");
        }
        if (senderUserId == null || senderUserId.trim().isEmpty()) {
            throw new DomainException("senderUserId requis");
        }
        if (senderUsername == null || senderUsername.trim().isEmpty()) {
            throw new DomainException("senderUsername requis");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new DomainException("Contenu requis");
        }

        ChatMessage m = new ChatMessage();
        m.id = UUID.randomUUID().toString();
        m.conversationId = conversationId;
        m.type = MessageType.CHAT;
        m.senderUserId = senderUserId.trim();
        m.senderUsername = senderUsername.trim();
        m.content = content.trim();
        m.timestamp = Instant.now();
        m.replyTo = replyTo;
        return m;
    }
    public static ChatMessage hydrate(String id,
                                      String conversationId,
                                      MessageType type,
                                      String senderUserId,
                                      String senderUsername,
                                      String content,
                                      Instant timestamp,
                                      Instant editedAt,
                                      Instant deletedAt,
                                      List<Reaction> reactions,
                                      ReplyTo replyTo) {

        ChatMessage m = new ChatMessage();
        m.id = id;
        m.conversationId = conversationId;
        m.type = type;
        m.senderUserId = senderUserId;
        m.senderUsername = senderUsername;
        m.content = content;
        m.timestamp = timestamp;
        m.editedAt = editedAt;
        m.deletedAt = deletedAt;
        m.reactions = (reactions != null) ? reactions : new ArrayList<>();
        m.replyTo = replyTo;
        return m;
    }

    public static ChatMessage join(String conversationId, String senderUsername, Instant ts) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new DomainException("conversationId requis");
        }
        if (senderUsername == null || senderUsername.trim().isEmpty()) {
            throw new DomainException("senderUsername requis");
        }

        ChatMessage message = new ChatMessage();
        message.id = UUID.randomUUID().toString();
        message.conversationId = conversationId;
        message.type = MessageType.JOIN;
        message.senderUserId = null; // système
        message.senderUsername = senderUsername.trim();
        message.timestamp = (ts != null ? ts : Instant.now());
        message.content = "a rejoint le chat";
        return message;
    }

    public static ChatMessage leave(String conversationId, String senderUsername, Instant ts) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new DomainException("conversationId requis");
        }
        if (senderUsername == null || senderUsername.trim().isEmpty()) {
            throw new DomainException("senderUsername requis");
        }

        ChatMessage message = new ChatMessage();
        message.id = UUID.randomUUID().toString();
        message.conversationId = conversationId;
        message.type = MessageType.LEAVE;
        message.senderUserId = null; // système
        message.senderUsername = senderUsername.trim();
        message.timestamp = (ts != null ? ts : Instant.now());
        message.content = "a quitté le chat";
        return message;
    }

    // ---------- Métier ----------

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isEditableBy(String userId) {
        return userId != null
                && deletedAt == null
                && MessageType.CHAT.equals(type)
                && senderUserId != null
                && senderUserId.equals(userId);
    }

    public void edit(String editorUserId, String newContent) {
        if (!MessageType.CHAT.equals(type)) {
            throw new DomainException("Seuls les messages CHAT sont modifiables");
        }
        if (deletedAt != null) {
            throw new DomainException("Message déjà supprimé");
        }
        if (senderUserId == null || !senderUserId.equals(editorUserId)) {
            throw new DomainException("Forbidden: vous ne pouvez modifier que vos messages");
        }
        if (newContent == null || newContent.trim().isEmpty()) {
            throw new DomainException("Contenu vide");
        }
        this.content = newContent.trim();
        this.editedAt = Instant.now();
    }

    public void delete(String requesterUserId) {
        if (!MessageType.CHAT.equals(type)) {
            throw new DomainException("Seuls les messages CHAT sont supprimables");
        }
        if (deletedAt != null) {
            return; // idempotent
        }
        if (senderUserId == null || !senderUserId.equals(requesterUserId)) {
            throw new DomainException("Forbidden: vous ne pouvez supprimer que vos messages");
        }

        this.deletedAt = Instant.now();
        this.content = "Message supprimé";
    }

    /**
     * Ajoute une réaction en évitant les doublons (Même user + Même emoji).
     */
    public void addReaction(Reaction reaction) {
        boolean alreadyExists = reactions.stream()
                .anyMatch(r -> r.getUserId().equals(reaction.getUserId())
                        && r.getEmoji().equals(reaction.getEmoji()));

        if (!alreadyExists) {
            this.reactions.add(reaction);
        }
    }


    public String getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public MessageType getType() {
        return type;
    }

    public String getSenderUserId() {
        return senderUserId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getContent() {
        return content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Instant getEditedAt() {
        return editedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public List<Reaction> getReactions() {
        return reactions;
    }

    // Setters utiles pour hydrater
    public void setReactions(List<Reaction> reactions) {
        this.reactions = (reactions != null) ? reactions : new ArrayList<>();
    }

    public ReplyTo getReplyTo() {
        return replyTo;
    }
}