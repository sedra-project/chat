package com.chat.adapter.websocket.dto.requests;

import com.chat.adapter.websocket.dto.ReplyToDto;

public class ConversationMessageRequest {

    private String content;
    private ReplyToDto replyTo;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ReplyToDto getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(ReplyToDto replyTo) {
        this.replyTo = replyTo;
    }
}
