package com.chat.adapter.websocket;

import com.chat.adapter.websocket.dto.ChatMessageDto;
import com.chat.adapter.websocket.dto.ReactionResponseDto;
import com.chat.adapter.websocket.dto.ReplyToDto;
import com.chat.adapter.websocket.dto.requests.ConversationMessageRequest;
import com.chat.adapter.websocket.dto.requests.DeleteMessageRequest;
import com.chat.adapter.websocket.dto.requests.EditMessageRequest;
import com.chat.adapter.websocket.dto.requests.ReactionRequest;
import com.chat.adapter.websocket.mapper.ReplyToMapper;
import com.chat.application.dto.NotificationDto;
import com.chat.application.dto.ReactionToggleResult;
import com.chat.application.port.in.DeleteMessageUseCase;
import com.chat.application.port.in.EditMessageUseCase;
import com.chat.application.port.in.SendToConversationUseCase;
import com.chat.application.port.in.ToggleReactionUseCase;
import com.chat.application.port.out.ConversationRepositoryPort;
import com.chat.application.port.out.NotificationPublisherRepositoryPort;
import com.chat.application.port.out.UserRepositoryPort;
import com.chat.domain.model.ChatMessage;
import com.chat.domain.model.ReplyTo;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

@Controller
public class ConversationWsController {

    private final SendToConversationUseCase sender;
    private final SimpMessagingTemplate messaging;
    private final ConversationRepositoryPort conversations;
    private final NotificationPublisherRepositoryPort notifier;
    private final ToggleReactionUseCase reactionService;
    private final UserRepositoryPort userRepositoryPort;
    private final EditMessageUseCase editMessage;
    private final DeleteMessageUseCase deleteMessage;

    public ConversationWsController(SendToConversationUseCase sender,
                                    SimpMessagingTemplate messaging,
                                    ConversationRepositoryPort conversations,
                                    NotificationPublisherRepositoryPort notifier,
                                    ToggleReactionUseCase addReactionUseCase,
                                    UserRepositoryPort userRepositoryPort,
                                    EditMessageUseCase editMessage,
                                    DeleteMessageUseCase deleteMessage) {
        this.sender = sender;
        this.messaging = messaging;
        this.conversations = conversations;
        this.notifier = notifier;
        this.reactionService = addReactionUseCase;
        this.userRepositoryPort = userRepositoryPort;
        this.editMessage = editMessage;
        this.deleteMessage = deleteMessage;
    }

    @MessageMapping("/conv.{id}.message")
    public void sendToConversation(@DestinationVariable("id") String conversationId,
                                   @Payload ConversationMessageRequest payload,
                                   Principal principal) {
        if (principal == null) throw new MessagingException("Unauthorized");
        if (payload == null || payload.getContent() == null) throw new MessagingException("Invalid payload");

        String userId = principal.getName();

        ReplyTo replyTo = ReplyToMapper.toDomainOrNull(payload.getReplyTo());

        // Envoi du message
        ChatMessage msg = sender.send(userId, conversationId, payload.getContent(), replyTo);
        messaging.convertAndSend("/topic/conv." + conversationId, ChatMessageDto.from(conversationId, msg));

        // Logique de Notification (toujours basée sur username)
        List<String> members = conversations.membersOf(conversationId);

        String preview = payload.getContent().trim();
        if (preview.length() > 80) preview = preview.substring(0, 80) + "...";

        String senderUsername = userRepositoryPort.findById(userId)
                .map(u -> u.getUsername().value())
                .orElseThrow(() -> new MessagingException("Unauthorized"));

        NotificationDto notification = NotificationDto.dmMessage(conversationId, senderUsername, preview);

        if (members != null) {
            for (String recipient : members) {
                if (!recipient.equals(senderUsername)) {
                    notifier.notifyUser(recipient, notification);
                    messaging.convertAndSendToUser(recipient, "/queue/notifications", notification);
                }
            }
        }
    }

    @MessageMapping("/conv.{id}.reaction")
    public void handleReaction(@DestinationVariable("id") String conversationId,
                               @Payload ReactionRequest payload,
                               Principal principal) {
        if (principal == null) throw new MessagingException("Unauthorized");
        if (payload == null || payload.getMessageId() == null || payload.getEmoji() == null) {
            throw new MessagingException("Invalid payload");
        }

        String userId = principal.getName();

        ReactionToggleResult result = reactionService.execute(
                userId,
                conversationId,
                payload.getMessageId(),
                payload.getEmoji()
        );

        messaging.convertAndSend(
                "/topic/conv." + conversationId + ".reactions",
                ReactionResponseDto.from(result)
        );
    }

    @MessageMapping("/conv.{id}.message.edit")
    public void editInConversation(@DestinationVariable("id") String conversationId,
                                   @Payload EditMessageRequest payload,
                                   Principal principal) {
        if (principal == null) throw new MessagingException("Unauthorized");
        if (payload == null || payload.getMessageId() == null || payload.getContent() == null) {
            throw new MessagingException("Invalid payload");
        }

        String userId = principal.getName();
        editMessage.edit(conversationId, payload.getMessageId(), userId, payload.getContent());
    }

    @MessageMapping("/conv.{id}.message.delete")
    public void deleteInConversation(@DestinationVariable("id") String conversationId,
                                     @Payload DeleteMessageRequest payload,
                                     Principal principal) {
        if (principal == null) throw new MessagingException("Unauthorized");
        if (payload == null || payload.getMessageId() == null) {
            throw new MessagingException("Invalid payload");
        }

        String userId = principal.getName();
        deleteMessage.delete(conversationId, payload.getMessageId(), userId);
    }
}