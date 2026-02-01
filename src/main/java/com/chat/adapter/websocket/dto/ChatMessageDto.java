package com.chat.adapter.websocket.dto;

import com.chat.domain.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChatMessageDto {

    private String id;
    private String type;

    private String senderUserId;
    private String senderUsername;

    private ReplyToDto replyTo;

    private String content;
    private long timestamp;
    private String conversationId;

    private List<ReactionResponseDto> reactions;

    public static ChatMessageDto from(String conversationId, ChatMessage message) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.id = message.getId();
        dto.type = message.getType() != null ? message.getType().name() : null;

        dto.senderUserId = message.getSenderUserId();
        dto.senderUsername = message.getSenderUsername();


        if (message.getReplyTo() != null) {
            dto.replyTo = new ReplyToDto(
                    message.getReplyTo().getMessageId(),
                    message.getReplyTo().getSenderUsername(),
                    message.getReplyTo().getExcerpt()
            );
        } else {
            dto.replyTo = null;
        }

        dto.content = message.getContent();
        dto.timestamp = message.getTimestamp() != null
                ? message.getTimestamp().toEpochMilli()
                : System.currentTimeMillis();

        dto.conversationId = conversationId;

        if (message.getReactions() != null) {
            dto.reactions = message.getReactions().stream()
                    .map(ReactionResponseDto::from)
                    .collect(Collectors.toList());
        } else {
            dto.reactions = new ArrayList<>();
        }

        return dto;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getSenderUserId() {
        return senderUserId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public ReplyToDto getReplyTo() {
        return replyTo;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public List<ReactionResponseDto> getReactions() {
        return reactions;
    }

    public void setReactions(List<ReactionResponseDto> reactions) {
        this.reactions = reactions;
    }
}