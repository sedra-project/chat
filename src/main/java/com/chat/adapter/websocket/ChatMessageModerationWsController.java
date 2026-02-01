package com.chat.adapter.websocket;

import com.chat.adapter.websocket.dto.requests.DeleteMessageRequest;
import com.chat.adapter.websocket.dto.requests.EditMessageRequest;
import com.chat.application.port.in.DeleteMessageUseCase;
import com.chat.application.port.in.EditMessageUseCase;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatMessageModerationWsController {

    private final EditMessageUseCase editMessage;
    private final DeleteMessageUseCase deleteMessage;

    public ChatMessageModerationWsController(EditMessageUseCase editMessage,
                                             DeleteMessageUseCase deleteMessage) {
        this.editMessage = editMessage;
        this.deleteMessage = deleteMessage;
    }

    @MessageMapping("/chat.message.edit")
    public void editPublic(@Payload EditMessageRequest payload, Principal principal) {
        if (principal == null) throw new MessagingException("Unauthorized");
        if (payload == null || payload.getMessageId() == null || payload.getContent() == null) {
            throw new MessagingException("Invalid payload");
        }

        String userId = principal.getName();
        editMessage.edit("public", payload.getMessageId(), userId, payload.getContent());
        // L’event est publié par le use case via MessageEventPublisherPort
    }

    @MessageMapping("/chat.message.delete")
    public void deletePublic(@Payload DeleteMessageRequest payload, Principal principal) {
        if (principal == null) throw new MessagingException("Unauthorized");
        if (payload == null || payload.getMessageId() == null) {
            throw new MessagingException("Invalid payload");
        }

        String userId = principal.getName();
        deleteMessage.delete("public", payload.getMessageId(), userId);
    }
}