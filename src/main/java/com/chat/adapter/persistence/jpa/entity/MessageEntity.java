package com.chat.adapter.persistence.jpa.entity;

import com.chat.domain.model.MessageType;

import javax.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "chat_messages",
        indexes = {
                @Index(name = "idx_conv_ts", columnList = "conversation_id, timestamp DESC"),
                @Index(name = "idx_conv_id", columnList = "conversation_id"),
                @Index(name = "idx_sender_user_id", columnList = "sender_user_id")
        }
)
public class MessageEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "conversation_id", nullable = false, length = 36)
    private String conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageType type;

    @Column(name = "sender_user_id", length = 36)
    private String senderUserId;

    @Column(name = "sender_username", length = 50)
    private String senderUsername;

    @Column(name = "reply_to_message_id", length = 36)
    private String replyToMessageId;

    @Column(name = "reply_to_sender_username", length = 50)
    private String replyToSenderUsername;

    @Column(name = "reply_to_excerpt", length = 160)
    private String replyToExcerpt;

    @Column(length = 1000)
    private String content;

    @Column(nullable = false)
    private Instant timestamp;

    private Instant editedAt;
    private Instant deletedAt;

    @Version
    private long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "message_reactions",
            joinColumns = @JoinColumn(name = "message_id")
    )
    private List<ReactionEmbeddable> reactions = new ArrayList<>();


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(String senderUserId) {
        this.senderUserId = senderUserId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(String replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public String getReplyToSenderUsername() {
        return replyToSenderUsername;
    }

    public void setReplyToSenderUsername(String replyToSenderUsername) {
        this.replyToSenderUsername = replyToSenderUsername;
    }

    public String getReplyToExcerpt() {
        return replyToExcerpt;
    }

    public void setReplyToExcerpt(String replyToExcerpt) {
        this.replyToExcerpt = replyToExcerpt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Instant getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Instant editedAt) {
        this.editedAt = editedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public List<ReactionEmbeddable> getReactions() {
        return reactions;
    }

    public void setReactions(List<ReactionEmbeddable> reactions) {
        this.reactions = reactions;
    }
}