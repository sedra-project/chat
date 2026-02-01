package com.chat.adapter.websocket;

import com.chat.adapter.websocket.dto.ChatMessageDto;
import com.chat.adapter.websocket.dto.ReactionResponseDto;
import com.chat.adapter.websocket.dto.ReplyToDto;
import com.chat.adapter.websocket.dto.requests.ChatMessageRequest;
import com.chat.adapter.websocket.dto.requests.ReactionRequest;
import com.chat.adapter.websocket.mapper.ReplyToMapper;
import com.chat.application.dto.ReactionToggleResult;
import com.chat.application.port.in.SendMessageUseCase;
import com.chat.application.port.in.ToggleReactionUseCase;
import com.chat.domain.model.ChatMessage;
import com.chat.domain.model.ReplyTo;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWsController {

    private final SendMessageUseCase send;
    private final ToggleReactionUseCase addReaction;
    private final SimpMessagingTemplate messaging;

    public ChatWsController(SendMessageUseCase send,
                            ToggleReactionUseCase addReaction,
                            SimpMessagingTemplate messaging) {
        this.send = send;
        this.addReaction = addReaction;
        this.messaging = messaging;
    }

    @MessageMapping("/chat.message")
    public void handleMessage(@Payload ChatMessageRequest payload, Principal principal) {
        if (principal == null) throw new MessagingException("Unauthorized");
        if (payload == null || payload.getContent() == null) throw new MessagingException("Invalid payload");

        String userId = principal.getName();

        ReplyTo replyTo = ReplyToMapper.toDomainOrNull(payload.getReplyTo());

        ChatMessage msg = send.send(userId, "public", payload.getContent(), replyTo);
        messaging.convertAndSend("/topic/public", ChatMessageDto.from("public", msg));
    }

    @MessageMapping("/chat.reaction")
    public void handlePublicReaction(@Payload ReactionRequest payload, Principal principal) {
        if (principal == null) throw new MessagingException("Unauthorized");
        if (payload == null || payload.getMessageId() == null || payload.getEmoji() == null) {
            throw new MessagingException("Invalid payload");
        }

        String userId = principal.getName();

        ReactionToggleResult result = addReaction.execute(
                userId,
                "public",
                payload.getMessageId(),
                payload.getEmoji()
        );

        messaging.convertAndSend("/topic/public.reactions", ReactionResponseDto.from(result));
    }
}