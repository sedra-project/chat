package com.chat.adapter.persistence.jpa.mapper;

import com.chat.adapter.persistence.jpa.entity.MessageEntity;
import com.chat.adapter.persistence.jpa.entity.ReactionEmbeddable;
import com.chat.domain.model.ChatMessage;
import com.chat.domain.model.Reaction;
import com.chat.domain.model.ReplyTo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChatMessageJpaMapper {

    private ChatMessageJpaMapper() {
    }

    public static MessageEntity toEntity(ChatMessage messageDomain) {
        if (messageDomain == null) return null;

        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setId(messageDomain.getId());
        messageEntity.setConversationId(messageDomain.getConversationId());
        messageEntity.setType(messageDomain.getType());
        messageEntity.setSenderUserId(messageDomain.getSenderUserId());
        messageEntity.setSenderUsername(messageDomain.getSenderUsername());
        messageEntity.setContent(messageDomain.getContent());
        messageEntity.setTimestamp(messageDomain.getTimestamp());
        messageEntity.setEditedAt(messageDomain.getEditedAt());
        messageEntity.setDeletedAt(messageDomain.getDeletedAt());


        if (messageDomain.getReplyTo() != null) {
            messageEntity.setReplyToMessageId(messageDomain.getReplyTo().getMessageId());
            messageEntity.setReplyToSenderUsername(messageDomain.getReplyTo().getSenderUsername());
            messageEntity.setReplyToExcerpt(messageDomain.getReplyTo().getExcerpt());
        } else {
            messageEntity.setReplyToMessageId(null);
            messageEntity.setReplyToSenderUsername(null);
            messageEntity.setReplyToExcerpt(null);
        }

        List<ReactionEmbeddable> reactions = (messageDomain.getReactions() == null) ? new ArrayList<>() :
                messageDomain.getReactions().stream()
                        .map(r -> new ReactionEmbeddable(
                                r.getEmoji(),
                                r.getUserId(),
                                r.getUsername(),
                                r.getTimestamp()
                        ))
                        .collect(Collectors.toList());

        messageEntity.setReactions(reactions);
        return messageEntity;
    }

    public static ChatMessage toDomain(MessageEntity messageEntity) {
        if (messageEntity == null) return null;

        List<Reaction> domainReactions = (messageEntity.getReactions() == null) ? new ArrayList<>() :
                messageEntity.getReactions().stream()
                        .map(re -> new Reaction(
                                null,
                                messageEntity.getId(),
                                re.getUserId(),
                                re.getUsername(),
                                re.getEmoji(),
                                re.getTimestamp()
                        ))
                        .collect(Collectors.toList());

        // NEW: replyTo
        ReplyTo replyTo = null;
        if (messageEntity.getReplyToMessageId() != null) {
            replyTo = new ReplyTo(
                    messageEntity.getReplyToMessageId(),
                    messageEntity.getReplyToSenderUsername(),
                    messageEntity.getReplyToExcerpt()
            );
        }

        return ChatMessage.hydrate(
                messageEntity.getId(),
                messageEntity.getConversationId(),
                messageEntity.getType(),
                messageEntity.getSenderUserId(),
                messageEntity.getSenderUsername(),
                messageEntity.getContent(),
                messageEntity.getTimestamp(),
                messageEntity.getEditedAt(),
                messageEntity.getDeletedAt(),
                domainReactions,
                replyTo
        );
    }
}