package com.chat.adapter.websocket.dto;

import com.chat.domain.model.ChatMessage;

import java.time.Instant;

public class MessageEventDto {

    private String type;          // "message.updated" | "message.deleted"
    private String conversationId;
    private String messageId;
    private String content;
    private Instant editedAt;
    private Instant deletedAt;

    public MessageEventDto() {
    }

    public MessageEventDto(String type, String conversationId, String messageId,
                           String content, Instant editedAt, Instant deletedAt) {
        this.type = type;
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.content = content;
        this.editedAt = editedAt;
        this.deletedAt = deletedAt;
    }

    public static MessageEventDto updated(ChatMessage m) {
        return new MessageEventDto("message.updated", m.getConversationId(), m.getId(),
                m.getContent(), m.getEditedAt(), null);
    }

    public static MessageEventDto deleted(ChatMessage m) {
        return new MessageEventDto("message.deleted", m.getConversationId(), m.getId(),
                m.getContent(), null, m.getDeletedAt());
    }

    public String getType() {
        return type;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getContent() {
        return content;
    }

    public Instant getEditedAt() {
        return editedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}